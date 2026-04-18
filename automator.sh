#!/bin/bash

# ============================================
# 自动Git提交脚本
# 功能：每天自动在文件中添加一行内容并push
# ============================================

# 配置区域 - 请修改为你的实际路径
REPO_PATH="/Users/kerbear/IdeaProjects/AccessControlSystem"          # 你的仓库路径
FILE_NAME="daily_log.txt"               # 要修改的文件名
COMMIT_MESSAGE="Daily auto commit"      # 提交信息

# 切换到仓库目录
cd "$REPO_PATH" || exit 1

# 检查是否在git仓库中
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "错误: $REPO_PATH 不是一个Git仓库"
    exit 1
fi

# 生成今天的日期
TODAY=$(date "+%Y-%m-%d %H:%M:%S")

# 随机选择一句"无用的话"
RANDOM_PHRASES=(
   OM_PHRASES = [
       "🐛 修复了一个不存在的bug",
       "✨ 增加了没用的功能",
       "📝 更新了无关紧要的文档",
       "🎨 优化了看不见的代码",
       "🔧 调整了毫无意义的配置",
       "💡 添加了没人看的注释",
       "🚀 部署了空气更新",
       "🐎 提升了0.0001%的性能",
       "🧹 删除了不必要的空格",
       "📦 更新了不重要的依赖",
       "🔄 重新格式化了代码",
       "🎯 微调了无关参数",
       "⚡ 优化了心理上的速度",
       "🔒 增强了想象中的安全性",
       "🏗️ 重构了永远不会执行的代码",
       "🔥 删除了有用的代码",
       "💩 写了辣鸡代码",
       "🤔 思考了人生的意义",
       "☕ 喝了一杯咖啡",
       "😴 瞌睡中提交的代码",
       "🎉 庆祝又过了一天",
       "📅 保持活跃度",
       "🤖 机器人自动提交",
       "💚 修复了CI/CD（并没有CI/CD）",
       "🚧 施工中... 其实并没有施工",
       "📈 更新了GitHub小绿点",
       "🕐 为了保持连续提交记录",
)

# 随机选一句
RANDOM_INDEX=$((RANDOM % ${#RANDOM_PHRASES[@]}))
SELECTED_PHRASE="${RANDOM_PHRASES[$RANDOM_INDEX]}"

# 往文件中追加一行
echo "[$TODAY] $SELECTED_PHRASE" >> "$FILE_NAME"

echo "已添加: [$TODAY] $SELECTED_PHRASE"

# 添加文件到暂存区
git add "$FILE_NAME"

# 提交
git commit -m "$COMMIT_MESSAGE - $TODAY"

# 推送到远程仓库
git push

echo "✅ 自动提交完成！"

