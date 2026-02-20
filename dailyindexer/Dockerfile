FROM python:3.12.2-slim
WORKDIR /reindexer/script
COPY requirements.txt .
COPY ./script /reindexer/script
RUN pip3 install -r requirements.txt
CMD ["python3", "reindexerMain.py", "-d", "../data", "-r", "production", "-g"]
