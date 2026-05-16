import asyncio
import json
import httpx
import chainlit as cl

BACKEND_URL = "http://localhost:8080"
ROAST_STREAM_ENDPOINT = f"{BACKEND_URL}/api/resume/roast/stream"
CHAT_STREAM_ENDPOINT = f"{BACKEND_URL}/api/chat/response/stream"
DEFAULT_NER_BACKEND = "DJL_REGEX"


@cl.on_chat_start
async def on_chat_start():
    await cl.Message(
        content="Upload your resume and I'll roast it, or just ask me anything!"
    ).send()


@cl.on_message
async def on_message(message: cl.Message):
    files = [e for e in message.elements if hasattr(e, "path")]
    if files:
        await _process_resume(files[0])
    else:
        await _chat(message.content)


async def _chat(message: str) -> None:
    reply = cl.Message(content="")
    await reply.send()
    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            async with client.stream(
                "POST",
                CHAT_STREAM_ENDPOINT,
                json={"message": message},
                headers={"Content-Type": "application/json"},
            ) as response:
                response.raise_for_status()
                async for line in response.aiter_lines():
                    if line.startswith("data:"):
                        data = line[len("data:"):].strip()
                        if data == "[DONE]":
                            await response.aclose()
                            break
                        try:
                            chunk = json.loads(data)
                            content = chunk["choices"][0]["delta"].get("content") or ""
                            if content:
                                await reply.stream_token(content)
                        except (json.JSONDecodeError, KeyError, IndexError):
                            pass
    except Exception as e:
        await cl.Message(content=f"Something went wrong: {e}").send()


async def _process_resume(file: cl.File) -> None:
    status_msg = await cl.Message(content="Thinking...").send()
    roast_msg = cl.Message(content="")
    roast_started = False

    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            async with client.stream(
                "POST",
                f"{ROAST_STREAM_ENDPOINT}?nerInferenceBackend={DEFAULT_NER_BACKEND}",
                files={"file": (file.name, open(file.path, "rb"))},
            ) as response:
                response.raise_for_status()
                event_name = None

                async for line in response.aiter_lines():
                    if line.startswith("event:"):
                        event_name = line[len("event:"):].strip()

                    elif line.startswith("data:"):
                        data = line[len("data:"):].strip()

                        if event_name == "entities":
                            entities = json.loads(data)
                            table = _build_entities_table(entities)
                            status_msg.content = ""
                            await status_msg.update()
                            for line in table.split("\n"):
                                await status_msg.stream_token(line + "\n")
                                await asyncio.sleep(0.05)
                            event_name = None

                        elif data == "[DONE]":
                            await response.aclose()
                            break

                        else:
                            try:
                                chunk = json.loads(data)
                                content = chunk["choices"][0]["delta"].get("content") or ""
                                if content:
                                    if not roast_started:
                                        await roast_msg.send()
                                        roast_started = True
                                    await roast_msg.stream_token(content)
                            except (json.JSONDecodeError, KeyError, IndexError):
                                pass

                    elif line == "":
                        event_name = None

    except Exception as e:
        await cl.Message(content=f"Something went wrong: {e}").send()


def _build_entities_table(entities: list) -> str:
    if not entities:
        return "✅ No PII detected in your resume."

    rows = "\n".join(
        f"| {e['text'].replace(chr(10), ' ')} | {e['type']} | {e['confidence']} | {e['count']} |"
        for e in entities
    )
    return (
        "**Detected PII Entities**\n\n"
        "| Entity | Type | Confidence | Count |\n"
        "|--------|------|------------|-------|\n"
        f"{rows}"
    )
