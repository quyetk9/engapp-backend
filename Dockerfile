FROM node:22-alpine
WORKDIR /app
COPY package.json data.js db.js server.js ./
ENV PORT=3000 DB_FILE=/data/engapp.db
VOLUME /data
EXPOSE 3000
# Nho dat JWT_SECRET khi chay: docker run -e JWT_SECRET=chuoi-bi-mat-dai ...
CMD ["node", "--no-warnings", "server.js"]
