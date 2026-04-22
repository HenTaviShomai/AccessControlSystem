#!/bin/bash

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  权限系统 Docker 部署脚本${NC}"
echo -e "${GREEN}========================================${NC}"

# 1. 检查 Colima
echo -e "\n${YELLOW}[1/5] 检查 Colima...${NC}"
if ! command -v colima &> /dev/null; then
    echo -e "${RED}错误: Colima 未安装${NC}"
    echo "请运行: brew install colima"
    exit 1
fi

# 2. 启动 Colima
echo -e "\n${YELLOW}[2/5] 启动 Colima...${NC}"
colima status &> /dev/null
if [ $? -ne 0 ]; then
    echo "启动 Colima (CPU:4, 内存:4GB, 磁盘:50GB)..."
    colima start --cpu 4 --memory 4 --disk 50
else
    echo -e "${GREEN}Colima 已运行${NC}"
fi

# 3. 停止旧容器
echo -e "\n${YELLOW}[3/5] 停止旧容器...${NC}"
docker-compose down 2>/dev/null

# 4. 启动服务（Docker 会自动打包，不需要本地 mvn）
echo -e "\n${YELLOW}[4/5] 构建并启动服务...${NC}"
docker-compose up -d --build

# 5. 等待服务启动
echo -e "\n${YELLOW}[5/5] 等待服务启动...${NC}"
echo "等待 MySQL 初始化..."
sleep 15

# 检查服务状态
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}  服务状态${NC}"
echo -e "${GREEN}========================================${NC}"
docker-compose ps

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}  部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "API 地址: ${YELLOW}http://localhost:8080${NC}"
echo -e "Swagger: ${YELLOW}http://localhost:8080/swagger-ui.html${NC}"
echo -e "\n测试登录:"
echo -e "  ${YELLOW}http POST localhost:8080/auth/login username=admin password=123456${NC}"
echo -e "\n查看日志:"
echo -e "  ${YELLOW}docker-compose logs -f app${NC}"
echo -e "\n停止服务:"
echo -e "  ${YELLOW}docker-compose down${NC}"