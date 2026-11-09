"""
Script to generate ml/train_colab.ipynb
=======================================
Generates a complete, self-contained Google Colab notebook for MedAssist AI fine-tuning
with Unsloth (4-bit QLoRA) on a free T4 GPU.
"""

import json
from pathlib import Path

notebook = {
    "cells": [
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "# 🏥 MedAssist AI — Fine-Tuning Pipeline with Unsloth & QLoRA\n",
                "\n",
                "[![Open In Colab](https://colab.research.google.com/assets/colab-badge.svg)](https://colab.research.google.com/github/Prajwal-Bansode17/AI-Based-Medical-Chatbot/blob/feature/custom-ai-training/ml/train_colab.ipynb)\n",
                "\n",
                "This self-contained Google Colab notebook fine-tunes a lightweight 1B LLM (**Llama-3.2-1B-Instruct** or **Gemma-3-1b-it**) using **Unsloth QLoRA (4-bit)** on a free **NVIDIA T4 GPU**.\n",
                "\n",
                "### Pipeline Features:\n",
                "- **Fast 4-bit Quantization & Fast Patching:** ~2-5x faster training, ~70% less VRAM (comfortably fits T4 16GB VRAM).\n",
                "- **Multilingual Clinical Training:** Ingests `train.jsonl` (4,497 samples) and `val.jsonl` (500 samples) with rich English, Hindi, and Marathi representations.\n",
                "- **Clinical Safety & Emergency Guardrails:** Evaluates red-flag emergency triage (MI, Stroke FAST, Suicide crisis) alongside routine symptom and OTC guidance.\n",
                "- **Direct Export:** Generates GGUF (`q4_k_m`) for local Ollama deployment or pushes merged 16-bit weights to Hugging Face Hub."
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "## 1. Verify GPU Environment"
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Check available GPU (NVIDIA T4 on Colab free tier)\n",
                "!nvidia-smi"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "## 2. Install Unsloth and Required Libraries\n",
                "We install Unsloth and the latest optimized libraries (`bitsandbytes`, `xformers`, `peft`, `trl`, `accelerate`)."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "%%capture\n",
                "!pip install --no-deps unsloth\n",
                "!pip install --no-deps \"xformers<0.0.29.post4\" trl peft accelerate bitsandbytes datasets"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "## 3. Load Model and Tokenizer with 4-bit Quantization\n",
                "We load `unsloth/Llama-3.2-1B-Instruct` (or `unsloth/gemma-3-1b-it`) in 4-bit to minimize memory footprint while maintaining strong instruction-following capabilities."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "from unsloth import FastLanguageModel\n",
                "import torch\n",
                "\n",
                "max_seq_length = 2048\n",
                "dtype = None  # None for auto detection (Float16 on T4, Bfloat16 on Ampere)\n",
                "load_in_4bit = True  # Enable 4-bit QLoRA\n",
                "\n",
                "# Target model choice (Llama-3.2-1B-Instruct is ideal for fast clinical edge inference)\n",
                "model_name = \"unsloth/Llama-3.2-1B-Instruct\"\n",
                "\n",
                "print(f\"Loading {model_name} with 4-bit quantization...\")\n",
                "model, tokenizer = FastLanguageModel.from_pretrained(\n",
                "    model_name=model_name,\n",
                "    max_seq_length=max_seq_length,\n",
                "    dtype=dtype,\n",
                "    load_in_4bit=load_in_4bit,\n",
                ")\n",
                "print(\"Model loaded successfully!\")"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "## 4. Configure QLoRA Adapters\n",
                "Apply parameter-efficient fine-tuning (PEFT) with rank $r=16$ and alpha $\\alpha=32$ on attention and MLP projections."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "model = FastLanguageModel.get_peft_model(\n",
                "    model,\n",
                "    r=16,\n",
                "    target_modules=[\n",
                "        \"q_proj\", \"k_proj\", \"v_proj\", \"o_proj\",\n",
                "        \"gate_proj\", \"up_proj\", \"down_proj\"\n",
                "    ],\n",
                "    lora_alpha=32,\n",
                "    lora_dropout=0,       # 0 is optimized for Unsloth\n",
                "    bias=\"none\",          # \"none\" is optimized for Unsloth\n",
                "    use_gradient_checkpointing=\"unsloth\",  # Long context memory optimization\n",
                "    random_state=42,\n",
                "    use_rslora=False,\n",
                "    loftq_config=None,\n",
                ")\n",
                "print(\"QLoRA adapters successfully attached!\")"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "## 5. Ingest MedAssist AI Fine-Tuning Dataset\n",
                "We load our curated fine-tuning dataset:\n",
                "- `train.jsonl` (4,497 samples)\n",
                "- `val.jsonl` (500 samples)\n",
                "\n",
                "The code checks if the files are already present in `/content/`, otherwise clones them from the MedAssist AI repository."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "import os\n",
                "from pathlib import Path\n",
                "from datasets import load_dataset\n",
                "\n",
                "# Check if files are local or clone from GitHub branch\n",
                "train_path = \"train.jsonl\"\n",
                "val_path = \"val.jsonl\"\n",
                "\n",
                "if not (os.path.exists(train_path) and os.path.exists(val_path)):\n",
                "    print(\"Cloning dataset from GitHub repository...\")\n",
                "    !git clone --depth 1 --branch feature/custom-ai-training https://github.com/Prajwal-Bansode17/AI-Based-Medical-Chatbot.git\n",
                "    train_path = \"AI-Based-Medical-Chatbot/ml/data/finetune/train.jsonl\"\n",
                "    val_path = \"AI-Based-Medical-Chatbot/ml/data/finetune/val.jsonl\"\n",
                "\n",
                "print(f\"Loading train dataset from: {train_path}\")\n",
                "print(f\"Loading val dataset from:   {val_path}\")\n",
                "\n",
                "dataset = load_dataset(\"json\", data_files={\"train\": train_path, \"val\": val_path})\n",
                "print(\"\\nDataset Summary:\")\n",
                "print(dataset)\n",
                "print(\"\\nSample Entry:\")\n",
                "print(dataset[\"train\"][0])"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "## 6. Formatting Dataset with System Prompt & ChatML Template\n",
                "Convert each entry into the standard conversational chat template for training."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "SYSTEM_PROMPT = (\n",
                "    \"You are MedAssist AI, a compassionate, clinically safe medical assistant. \"\n",
                "    \"You explain complex medical concepts in simple layman terms across English, Hindi, and Marathi, \"\n",
                "    \"prioritize patient safety, flag medical emergencies immediately, and never provide unauthorized prescriptions. \"\n",
                "    \"Always advise consulting a qualified doctor for definitive diagnosis and treatment.\"\n",
                ")\n",
                "\n",
                "def format_prompts(batch):\n",
                "    instructions = batch[\"instruction\"]\n",
                "    outputs = batch[\"output\"]\n",
                "    texts = []\n",
                "    for instr, out in zip(instructions, outputs):\n",
                "        messages = [\n",
                "            {\"role\": \"system\", \"content\": SYSTEM_PROMPT},\n",
                "            {\"role\": \"user\", \"content\": instr},\n",
                "            {\"role\": \"assistant\", \"content\": out},\n",
                "        ]\n",
                "        # Use tokenizer chat template if available or standard ChatML format\n",
                "        if hasattr(tokenizer, \"apply_chat_template\") and tokenizer.chat_template is not None:\n",
                "            text = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=False)\n",
                "        else:\n",
                "            text = f\"<|im_start|>system\\n{SYSTEM_PROMPT}<|im_end|>\\n<|im_start|>user\\n{instr}<|im_end|>\\n<|im_start|>assistant\\n{out}<|im_end|>\"\n",
                "        texts.append(text)\n",
                "    return {\"text\": texts}\n",
                "\n",
                "train_data = dataset[\"train\"].map(format_prompts, batched=True)\n",
                "val_data = dataset[\"val\"].map(format_prompts, batched=True)\n",
                "print(\"Formatted Sample Text:\\n\", train_data[0][\"text\"][:400])"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "## 7. Initialize SFTTrainer and Train\n",
                "We train using `SFTTrainer` with 8-bit AdamW optimizer, warmup, cosine/linear scheduling, and periodic validation loss evaluation."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "from trl import SFTTrainer\n",
                "from transformers import TrainingArguments\n",
                "from unsloth import is_bfloat16_supported\n",
                "\n",
                "training_args = TrainingArguments(\n",
                "    per_device_train_batch_size=2,\n",
                "    gradient_accumulation_steps=4,\n",
                "    warmup_steps=10,\n",
                "    max_steps=150,               # ~1-2 epochs on high-yield clinical data\n",
                "    learning_rate=2e-4,\n",
                "    fp16=not is_bfloat16_supported(),\n",
                "    bf16=is_bfloat16_supported(),\n",
                "    logging_steps=10,\n",
                "    eval_strategy=\"steps\",\n",
                "    eval_steps=30,\n",
                "    optim=\"adamw_8bit\",\n",
                "    weight_decay=0.01,\n",
                "    lr_scheduler_type=\"linear\",\n",
                "    seed=42,\n",
                "    output_dir=\"outputs\",\n",
                "    report_to=\"none\",\n",
                ")\n",
                "\n",
                "trainer = SFTTrainer(\n",
                "    model=model,\n",
                "    tokenizer=tokenizer,\n",
                "    train_dataset=train_data,\n",
                "    eval_dataset=val_data,\n",
                "    dataset_text_field=\"text\",\n",
                "    max_seq_length=max_seq_length,\n",
                "    dataset_num_proc=2,\n",
                "    packing=False,\n",
                "    args=training_args,\n",
                ")\n",
                "\n",
                "print(\"Starting MedAssist AI fine-tuning...\")\n",
                "trainer_stats = trainer.train()\n",
                "print(\"Training Complete!\")"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "## 8. Plot Training & Validation Loss Curve"
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "import matplotlib.pyplot as plt\n",
                "\n",
                "log_history = trainer.state.log_history\n",
                "train_steps, train_loss = [], []\n",
                "eval_steps, eval_loss = [], []\n",
                "\n",
                "for log in log_history:\n",
                "    if \"loss\" in log and \"step\" in log:\n",
                "        train_steps.append(log[\"step\"])\n",
                "        train_loss.append(log[\"loss\"])\n",
                "    if \"eval_loss\" in log and \"step\" in log:\n",
                "        eval_steps.append(log[\"step\"])\n",
                "        eval_loss.append(log[\"eval_loss\"])\n",
                "\n",
                "plt.figure(figsize=(9, 5))\n",
                "plt.plot(train_steps, train_loss, label=\"Training Loss\", color=\"#008080\", lw=2)\n",
                "if eval_steps:\n",
                "    plt.plot(eval_steps, eval_loss, label=\"Validation Loss\", color=\"#FF6B6B\", lw=2, marker=\"o\")\n",
                "plt.title(\"MedAssist AI Fine-Tuning Loss Curve (Unsloth QLoRA)\", fontsize=14, fontweight=\"bold\")\n",
                "plt.xlabel(\"Training Steps\", fontsize=12)\n",
                "plt.ylabel(\"Loss\", fontsize=12)\n",
                "plt.legend(fontsize=11)\n",
                "plt.grid(True, linestyle=\"--\", alpha=0.6)\n",
                "plt.tight_layout()\n",
                "plt.show()"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "## 9. Sample Inference Verification (English & Marathi)\n",
                "We test the trained model on three critical clinical queries:\n",
                "1. Scientific-to-Layman term explanation (English)\n",
                "2. Regional symptom & home care guidance (Marathi)\n",
                "3. Acute emergency red-flag triage (Marathi)"
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "from transformers import TextStreamer\n",
                "\n",
                "FastLanguageModel.for_inference(model)\n",
                "streamer = TextStreamer(tokenizer, skip_prompt=True)\n",
                "\n",
                "test_queries = [\n",
                "    (\"English Layman Term\", \"What does cephalalgia mean in simple terms?\"),\n",
                "    (\"Marathi Symptom Advice\", \"मला दोन दिवसांपासून खूप ताप आहे आणि थंडी वाजत आहे, काय करू?\"),\n",
                "    (\"Marathi Emergency Triage\", \"छातीत डाव्या बाजूला असह्य कळ येऊन खूप घाम फुटत आहे, काय करावे?\"),\n",
                "]\n",
                "\n",
                "for title, query in test_queries:\n",
                "    print(f\"\\n{'='*60}\")\n",
                "    print(f\"TEST: {title}\")\n",
                "    print(f\"Query: {query}\")\n",
                "    print(f\"{'-'*60}\")\n",
                "    messages = [\n",
                "        {\"role\": \"system\", \"content\": SYSTEM_PROMPT},\n",
                "        {\"role\": \"user\", \"content\": query},\n",
                "    ]\n",
                "    if hasattr(tokenizer, \"apply_chat_template\") and tokenizer.chat_template is not None:\n",
                "        prompt = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)\n",
                "    else:\n",
                "        prompt = f\"<|im_start|>system\\n{SYSTEM_PROMPT}<|im_end|>\\n<|im_start|>user\\n{query}<|im_end|>\\n<|im_start|>assistant\\n\"\n",
                "    inputs = tokenizer([prompt], return_tensors=\"pt\").to(\"cuda\")\n",
                "    _ = model.generate(**inputs, streamer=streamer, max_new_tokens=300, temperature=0.3, top_p=0.9)"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "## 10. Export to GGUF (for Local Ollama Deployment)\n",
                "Export quantized GGUF format (`q4_k_m`) directly usable with Ollama inside the MedAssist AI backend (`ollama create medassist-1b -f Modelfile`)."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Save GGUF for Ollama deployment\n",
                "gguf_output_dir = \"medassist_ai_gguf\"\n",
                "print(f\"Exporting model to GGUF format ({gguf_output_dir})...\")\n",
                "\n",
                "# You can choose \"q4_k_m\" (recommended 4-bit) or \"q8_0\" or \"f16\"\n",
                "model.save_pretrained_gguf(gguf_output_dir, tokenizer, quantization_method=\"q4_k_m\")\n",
                "print(f\"GGUF export finished! Files in ./{gguf_output_dir}\")\n",
                "!ls -lh {gguf_output_dir}"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "## 11. Save LoRA Adapters & Push to Hugging Face Hub (Optional)\n",
                "Configure your Hugging Face Hub repository name and token to save your trained MedAssist AI model permanently."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Save LoRA adapter locally\n",
                "lora_output_dir = \"medassist_ai_lora_adapters\"\n",
                "model.save_pretrained(lora_output_dir)\n",
                "tokenizer.save_pretrained(lora_output_dir)\n",
                "print(f\"LoRA adapter saved locally to: {lora_output_dir}\")\n",
                "\n",
                "# --- Hugging Face Hub Push (Optional) ---\n",
                "# To push to HF, set your repository name and Hugging Face write token below:\n",
                "HF_REPO_NAME = \"your-username/MedAssist-AI-1B\"  # Change to your repo name\n",
                "HF_TOKEN = \"\"                                 # Paste your HF Write Token\n",
                "\n",
                "if HF_TOKEN and \"your-username\" not in HF_REPO_NAME:\n",
                "    print(f\"Pushing 16-bit merged model to Hugging Face Hub: {HF_REPO_NAME}...\")\n",
                "    model.push_to_hub_merged(HF_REPO_NAME, tokenizer, save_method=\"merged_16bit\", token=HF_TOKEN)\n",
                "    print(\"Model successfully pushed to Hugging Face Hub!\")\n",
                "else:\n",
                "    print(\"Skipping Hugging Face push (set HF_REPO_NAME and HF_TOKEN to upload).\")"
            ]
        }
    ],
    "metadata": {
        "accelerator": "GPU",
        "colab": {
            "gpuType": "T4",
            "provenance": []
        },
        "kernelspec": {
            "display_name": "Python 3",
            "name": "python3"
        },
        "language_info": {
            "name": "python"
        }
    },
    "nbformat": 4,
    "nbformat_minor": 0
}

output_path = Path(__file__).resolve().parent / "train_colab.ipynb"
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(notebook, f, indent=1, ensure_ascii=False)

print(f"Generated {output_path} successfully. Total cells: {len(notebook['cells'])}")
