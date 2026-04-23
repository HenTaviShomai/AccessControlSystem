#!/bin/bash

echo "========== 企业级权限系统 - Docker 启动脚本 =========="

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "错误: Docker 未安装，请先安装 Docker"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "错误: Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

# 停止并删除旧容器
echo "停止旧容器..."
docker-compose down

# 重新构建镜像
echo "重新构建镜像..."
docker-compose build --no-cache

# 启动服务
echo "启动服务..."
docker-compose up -d

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo "检查服务状态..."
docker-compose ps

# 查看日志
echo "查看应用日志..."
docker-compose logs app --tail=20

echo ""
echo "========== 启动完成 =========="
echo "应用地址: http://localhost:8080"
echo "Swagger文档: http://localhost:8080/swagger-ui.html"
echo ""
echo "查看实时日志: docker-compose logs -f app"
echo "停止服务: docker-compose down"