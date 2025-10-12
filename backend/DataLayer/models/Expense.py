from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime
from utils import PyObjectId
from .expense_item import ExpenseItem

class Expense(BaseModel):
    id: PyObjectId = Field(default_factory=PyObjectId, alias="_id")
    user_id: PyObjectId
    date: datetime
    total_amount: float
    category: str
    description: Optional[str] = None
    items: Optional[List[ExpenseItem]] = []
    ocr_text: Optional[str] = None
    photo_url: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

    class Config:
        allow_population_by_field_name = True
        json_encoders = {PyObjectId: str}
