from fastapi import FastAPI, Request, Response, Header
from pydantic import BaseModel
from typing import List
import aiohttp
import time
import asyncio
import logging
import os


SERVICE_PORT = os.getenv("PORT", "8000")
app = FastAPI()


@app.middleware("http")
async def add_process_time_header(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    process_time = time.time() - start_time
    print(f"Request processing time: {process_time:.4f} seconds")
    return response


@app.middleware("http")
async def add_port_header(request: Request, call_next):
    response = await call_next(request)
    response.headers["X-Service-Port"] = str(SERVICE_PORT)
    return response


# @app.middleware("http")
# async def debug_post(request: Request, call_next):
#     print(request.headers)
#     response = await call_next(request)
#     return response


users = [
    {"id": 1, "name": "Alice", "email": "alice@example.com"},
    {"id": 2, "name": "Bob", "email": "bob@example.com"},
    {"id": 3, "name": "Charlie", "email": "charlie@example.com"}
]


class UserModel(BaseModel):
    id: int
    name: str
    email: str


class CreateUserModel(BaseModel):
    name: str
    email: str


class MessageModel(BaseModel):
    content: str


class CommentModel(BaseModel):
    text: str
    author: str


@app.get("/api/v1/users/", response_model=List[UserModel])
async def get_users():
    return users


@app.post("/api/v1/users/", response_model=UserModel)
async def create_user(user: CreateUserModel, x_forwarded_by: str = Header(None)):
    new_user = {
        "id": len(users) + 1,
        "name": user.name,
        "email": user.email
    }
    users.append(new_user)

    print("hi")

    another_port = "8001" if SERVICE_PORT == "8000" else "8000"

    if not x_forwarded_by or x_forwarded_by != str(another_port):
        print("request")
        async with aiohttp.ClientSession() as session:
            await session.post(f"http://172.17.0.1:{another_port}/api/v1/users/", json=user.dict(), headers={"X-Forwarded-By": SERVICE_PORT})

    return new_user


@app.get("/admin/")
async def admin_info():
    return {
        "service": "User Management API",
        "version": "1.0",
        "status": "running",
        "port": SERVICE_PORT
    }


@app.post("/send_message/")
async def send_message(message: MessageModel):
    await asyncio.sleep(1)
    logging.info(f"Received message: {message.content}")
    return {"status": "message processed"}


@app.post("/comment/create/")
async def create_comment(comment: CommentModel):
    logging.info(f"Comment successfully created by {comment.author}: {comment.text}")
    return {"status": "comment created"}
