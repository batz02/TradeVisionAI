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