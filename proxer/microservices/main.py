from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import asyncio
import logging
import os


app = FastAPI()


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
async def create_user(user: CreateUserModel):
    new_user = {
       "id": len(users) + 1,
       "name": user.name,
       "email": user.email
   }

    users.append(new_user)

    return new_user


@app.get("/admin/")
async def admin_info():
    return {
        "service": "User Management API",
        "version": "1.0",
        "status": "running",
        "port": os.getenv("PORT", -1)
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
