from models import Expense
from db_client import db
from pymongo.collection import Collection
from bson import ObjectId
from datetime import datetime

expenses_col: Collection = db["expenses"]


def add_expense(user_id: str, date: datetime, total_amount: float, category: str,
                description: str = "", ocr_text: str = None,
                photo_url: str = None, items: list = None) -> str:
    expense = Expense(
        user_id=ObjectId(user_id),
        date=date,
        total_amount=total_amount,
        category=category,
        description=description,
        ocr_text=ocr_text,
        photo_url=photo_url,
        items=items or []
    )
    result = expenses_col.insert_one(expense.dict(by_alias=True))
    return str(result.inserted_id)


def get_expenses_by_user(user_id: str) -> list[Expense]:
    docs = expenses_col.find({"user_id": ObjectId(user_id)})
    return [Expense(**doc) for doc in docs]


def get_expenses_by_user_and_month(user_id: str, year: int, month: int) -> list[Expense]:
    start_date = datetime(year, month, 1)
    end_date = datetime(year + 1, 1, 1) if month == 12 else datetime(year, month + 1, 1)
    docs = expenses_col.find({
        "user_id": ObjectId(user_id),
        "date": {"$gte": start_date, "$lt": end_date}
    })
    return [Expense(**doc) for doc in docs]
