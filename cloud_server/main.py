import io
import os
import numpy as np
import tensorflow as tf
from fastapi import FastAPI, UploadFile, File, Depends, HTTPException, Security, Query
from fastapi.security.api_key import APIKeyHeader
from starlette import status
from PIL import Image

app = FastAPI()

API_KEY = os.getenv("API_KEY_SECRET")
API_KEY_NAME = "X-API-KEY"
api_key_header = APIKeyHeader(name=API_KEY_NAME, auto_error=False)

async def get_api_key(header_value: str = Security(api_key_header)):
    if API_KEY and header_value == API_KEY:
        return header_value
    raise HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail="Accesso negato: API Key non valida o non configurata"
    )

MODELS = {}
MODEL_FILES = {
    "inception": "inception_v3.h5",
    "mobilenet": "mobilenet_v2.h5",
    "resnet": "resnet_50.h5"
}

@app.on_event("startup")
def load_all_models():
    for m_id, m_path in MODEL_FILES.items():
        if os.path.exists(m_path):
            print(f"Caricamento modello: {m_id}...")
            MODELS[m_id] = tf.keras.models.load_model(m_path, compile=False) 
    print("Tutti i modelli sono pronti!")

@app.post("/predict")
async def predict(
    model_id: str = Query(...), 
    file: UploadFile = File(...),
    _auth: str = Depends(get_api_key)
):
    if model_id not in MODELS:
        raise HTTPException(status_code=404, detail="Modello non trovato sul server")

    current_model = MODELS[model_id]
    
    contents = await file.read()
    img = Image.open(io.BytesIO(contents)).convert('RGB')
    
    input_shape = current_model.input_shape
    img = img.resize((input_shape[1], input_shape[2]))
    img_array = np.array(img, dtype=np.float32) / 255.0
    img_array = np.expand_dims(img_array, axis=0)

    predictions = current_model(img_array, training=False).numpy()
    class_idx = np.argmax(predictions[0])
    confidence = float(np.max(predictions[0]))
    
    return {
        "model_used": model_id,
        "prediction": "BUY" if class_idx == 1 else "SELL",
        "confidence": round(confidence * 100, 2)
    }