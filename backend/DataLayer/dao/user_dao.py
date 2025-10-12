from models import User
from db_client import db
from pymongo.collection import Collection

users_col: Collection = db["users"]

def create_user(email: str, username: str, password_hash: str) -> str:
    user = User(email=email, username=username, password_hash=password_hash)
    result = users_col.insert_one(user.dict(by_alias=True))
    return str(result.inserted_id)

def get_user_by_email(email: str) -> User | None:
    doc = users_col.find_one({"email": email})
    if doc:
        return User(**doc)
    return None
