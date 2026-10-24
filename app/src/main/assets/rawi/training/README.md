# Training provenance

The supplied inference weights are the pinned `rawi_v3.int8.onnx` export from
TigreGotico/rawi-v3. Training source/notebook provenance is the upstream
`rawi_v3_train.ipynb` at revision `b53b4c28a5e432bc041cf2ba4246d78666eb5355`:
<https://huggingface.co/TigreGotico/rawi-v3/tree/b53b4c28a5e432bc041cf2ba4246d78666eb5355>.
TigreGotico authored the corpus/model/notebook; Mike Hansen provided GPU
training compute. No training code or unpinned weights are needed at runtime.
