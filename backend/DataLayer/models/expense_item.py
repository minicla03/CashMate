from pydantic import BaseModel

class ExpenseItem(BaseModel):
    name: str
    price: float
