FROM python:3.12-slim
RUN groupadd --gid 10001 mocks && useradd --uid 10001 --gid mocks --create-home mocks \
    && mkdir /data && chown mocks:mocks /data
WORKDIR /app
COPY server.py ./
USER mocks
EXPOSE 4101 4102 4103
CMD ["python", "-u", "server.py", "--bind", "0.0.0.0", "--database", "/data/mock.db"]
