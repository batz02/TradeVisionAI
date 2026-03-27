import io
import os
import base64
import numpy as np
import yfinance as yf
import tensorflow as tf
import mplfinance as mpf
import pandas as pd
from fastapi import FastAPI, UploadFile, File, Depends, HTTPException, Security, Query, Body
from fastapi.security.api_key import APIKeyHeader
from starlette import status
from PIL import Image
from pydantic import BaseModel

app = FastAPI()

API_KEY = os.getenv("API_KEY_SECRET")
API_KEY_NAME = "X-API-KEY"
api_key_header = APIKeyHeader(name=API_KEY_NAME, auto_error=False)

async def get_api_key(header_value: str = Security(api_key_header)):
    if API_KEY and header_value == API_KEY:
        return header_value
    raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="API Key Errata")

MODELS = {}
MODEL_FILES = {
    "inception": "inception_model",
    "mobilenet": "mobilenet_model"
}

@app.on_event("startup")
def load_all_models():
    for m_id, m_path in MODEL_FILES.items():
        if os.path.exists(m_path) and os.path.isdir(m_path):
            print(f"Caricamento modello via TFSMLayer: {m_id}...")
            try:
                MODELS[m_id] = tf.keras.layers.TFSMLayer(m_path, call_endpoint='serving_default')
                print(f"-> {m_id} caricato con successo!")
            except Exception as e:
                print(f"Errore critico caricando {m_id}: {e}")
        else:
            print(f"ATTENZIONE: Cartella {m_path} non trovata!")
            
    print(f"Inizializzazione completata. Modelli in memoria: {list(MODELS.keys())}")

class TickerRequest(BaseModel):
    ticker: str
    model_id: str

@app.post("/analyze_ticker")
async def analyze_ticker(
    request: TickerRequest,
    _auth: str = Depends(get_api_key)
):
    if request.model_id not in MODELS:
        raise HTTPException(status_code=404, detail="Modello non trovato in RAM")

    ticker_data = yf.Ticker(request.ticker)
    df = ticker_data.history(period="7d", interval="1d")
    
    if df.empty:
        raise HTTPException(status_code=400, detail="Dati non trovati per questo Ticker")

    buf = io.BytesIO()
    
    mc = mpf.make_marketcolors(up='green', down='red', edge='inherit', wick='black')
    s = mpf.make_mpf_style(marketcolors=mc, gridstyle='')
    
    mpf.plot(df, 
             type='candle', 
             style=s, 
             axisoff=True, 
             savefig=dict(fname=buf, format='jpg', bbox_inches='tight', pad_inches=0))
    
    buf.seek(0)
    
    image_base64 = base64.b64encode(buf.getvalue()).decode('utf-8')

    current_model = MODELS[request.model_id]
    
    img = Image.open(buf).convert('RGB')
    target_size = (224, 224) 
    img = img.resize(target_size)
    
    img_array = np.array(img, dtype=np.float32) / 255.0
    img_array = np.expand_dims(img_array, axis=0)
    tensor_input = tf.convert_to_tensor(img_array)
    
    raw_predictions = current_model(tensor_input)
    
    if isinstance(raw_predictions, dict):
        first_key = list(raw_predictions.keys())[0]
        predictions = raw_predictions[first_key].numpy()
    else:
        predictions = raw_predictions.numpy()
    
    class_idx = np.argmax(predictions[0])
    confidence = float(np.max(predictions[0]))
    
    return {
        "label": "COMPRA" if class_idx == 1 else "VENDI",
        "confidence": round(confidence * 100, 2),
        "image_base64": image_base64
    }

@app.post("/predict")
async def predict(
    model_id: str = Query(...),
    file: UploadFile = File(...),
    _auth: str = Depends(get_api_key)
):
    if model_id not in MODELS:
        raise HTTPException(status_code=404, detail="Modello non trovato in RAM")

    current_model = MODELS[model_id]
    
    contents = await file.read()
    img = Image.open(io.BytesIO(contents)).convert('RGB')
    
    target_size = (224, 224) 

    img = img.resize(target_size)
    img_array = np.array(img, dtype=np.float32) / 255.0
    img_array = np.expand_dims(img_array, axis=0)

    tensor_input = tf.convert_to_tensor(img_array)
    
    raw_predictions = current_model(tensor_input)
    
    if isinstance(raw_predictions, dict):
        first_key = list(raw_predictions.keys())[0]
        predictions = raw_predictions[first_key].numpy()
    else:
        predictions = raw_predictions.numpy()
    
    class_idx = np.argmax(predictions[0])
    confidence = float(np.max(predictions[0]))
    
    return {
        "model_used": model_id,
        "prediction": "COMPRA" if class_idx == 1 else "VENDI",
        "confidence": round(confidence * 100, 2)
        }