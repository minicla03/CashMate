from pymongo import MongoClient
from dotenv import load_dotenv
import os

load_dotenv()

MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017")
DB_NAME = os.getenv("DB_NAME", "cashmate")

client = MongoClient(MONGO_URI)

db = client[DB_NAME]

def test_connection():
    try:
        client.admin.command('ping')
        print(f"✅ Connessione a MongoDB riuscita! Database: {DB_NAME}")
    except Exception as e:
        print(f"❌ Connessione a MongoDB fallita: {e}")
