#!/bin/bash
# hadoop-cluster-setup.sh
# 优雅的 Hadoop 集群一键搭建脚本
# 特点：模块化设计、优雅的进度显示、详细日志、错误恢复、配置模板
# 版本：3.0.0

# 更严格的错误处理
set -Eeuo pipefail  # 使用 -E 以便 trap 捕获 ERR 信号
trap 'handle_error $? $LINENO "$BASH_COMMAND"' ERR
trap 'cleanup_on_exit' EXIT  # 确保清理函数在退出时执行

# ==================== 配置区域 ====================
# 这里可以修改配置，也可以使用配置文件覆盖

# 集群配置
CLUSTER_NAME="widdonexus-hadoop-cluster"
HADOOP_VERSION="3.3.6"
JAVA_VERSION="11"

# 集群节点配置（支持主机名或IP）
MASTER_NODE="hadoop102"
WORKER_NODES="hadoop103 hadoop104"
ALL_NODES="$MASTER_NODE $WORKER_NODES"

# 服务分配（根据您的规划）
# hadoop102: NameNode + DataNode
# hadoop103: DataNode + ResourceManager + NodeManager + JobHistory
# hadoop104: DataNode + SecondaryNameNode + NodeManager
NAMENODE_NODE="hadoop102"
RESOURCEMANAGER_NODE="hadoop103"
SECONDARY_NODE="hadoop104"
JOBHISTORY_NODE="hadoop103"

# 用户配置
HADOOP_USER="hadoop"
HADOOP_GROUP="hadoop"
HADOOP_PASSWORD="widdonexus@hadoop"

# 目录配置
HADOOP_HOME="/opt/module/hadoop${HADOOP_VERSION}"
JAVA_HOME="/opt/module/java-$JAVA_VERSION-openjdk"
DATA_DIR="/data/hadoop"
LOG_DIR="/var/log/hadoop"
PID_DIR="/var/run/hadoop"

# 网络配置（端口规划）
PORTS=(
    "9820:NameNode RPC"
    "9870:NameNode HTTP"
    "9866:DataNode RPC"
    "9864:DataNode HTTP"
    "9868:SecondaryNameNode HTTP"
    "8088:ResourceManager HTTP"
    "8042:NodeManager HTTP"
    "19888:JobHistory HTTP"
    "10020:JobHistory RPC"
)

# 下载镜像（可选国内镜像）
#HADOOP_MIRROR="https://dlcdn.apache.org/hadoop/common"
# 国内镜像：
HADOOP_MIRROR="https://mirrors.bfsu.edu.cn/apache/hadoop/common"

# 颜色定义
BLACK='\033[0;30m'
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[0;37m'
BOLD='\033[1m'
DIM='\033[2m'
ITALIC='\033[3m'
UNDERLINE='\033[4m'
BLINK='\033[5m'
REVERSE='\033[7m'
HIDDEN='\033[8m'
NC='\033[0m' # No Color

# 全局变量
LOG_FILE="/tmp/hadoop-setup-$(date +%Y%m%d-%H%M%S).log"
CONFIG_FILE="/tmp/hadoop-config-$(date +%s).conf"
STEP=0
TOTAL_STEPS=15
SUCCESS_COUNT=0
ERROR_COUNT=0
START_TIME=$(date +%s)

# ==================== 美观的输出函数 ====================

# 打印横幅
print_banner() {
    clear
    echo -e "${BLUE}${BOLD}"
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║                                                          ║"
    echo "║   ██╗  ██╗ █████╗ ██████╗  ██████╗  ██████╗ ██████╗      ║"
    echo "║   ██║  ██║██╔══██╗██╔══██╗██╔═══██╗██╔═══██╗██╔══██╗     ║"
    echo "║   ███████║███████║██║  ██║██║   ██║██║   ██║██████╔╝     ║"
    echo "║   ██╔══██║██╔══██║██║  ██║██║   ██║██║   ██║██╔═══╝      ║"
    echo "║   ██║  ██║██║  ██║██████╔╝╚██████╔╝╚██████╔╝██║          ║"
    echo "║   ╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝  ╚═════╝  ╚═════╝ ╚═╝          ║"
    echo "║                                                          ║"
    echo "║               		   v1.1.0                  			 ║"
    echo "║                      widdonexus                          ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# 显示主菜单
show_main_menu() {
    print_banner

    echo -e "${YELLOW}${BOLD}请选择操作模式：${NC}"
    echo -e "  ${GREEN}[1]${NC} ${BOLD}安装集群${NC}    - 全新安装或重新安装Hadoop集群"
    echo -e "  ${RED}[2]${NC} ${BOLD}卸载集群${NC}    - 完全卸载现有Hadoop集群"
    echo -e "  ${CYAN}[3]${NC} ${BOLD}检查状态${NC}    - 检查集群运行状态"
    echo -e "  ${GREEN}[4]${NC} ${BOLD}启动集群${NC}    - 启动Hadoop所有服务"
    echo -e "  ${RED}[5]${NC} ${BOLD}停止集群${NC}    - 停止Hadoop所有服务"
    echo -e "  ${YELLOW}[6]${NC} ${BOLD}修复集群${NC}    - 修复Java环境和数据目录问题"
    echo -e "  ${BLUE}[7]${NC} ${BOLD}退出脚本${NC}"

    echo -e "\n${YELLOW}请输入选择 (1-7): ${NC}\c"
    read -r main_choice

    case $main_choice in
        1)
            MODE="install"
            ;;
        2)
            MODE="uninstall"
            ;;
        3)
            MODE="status"
            ;;
		4)
            MODE="start"
            ;;
        5)
            MODE="stop"
            ;;
        6)
            MODE="fix"
            ;;
        7)
            echo -e "${GREEN}退出脚本。${NC}"
            exit 0
            ;;
        *)
            print_error "无效选择，默认使用安装模式"
            MODE="install"
            ;;
    esac
}

# 集群状态检查（新增）
check_cluster_status() {
    print_banner

    print_step "集群状态检查" "检查Hadoop集群运行状态"

    echo -e "\n${CYAN}${BOLD}📊 集群基本信息：${NC}"
    echo -e "  集群名称: ${YELLOW}$CLUSTER_NAME${NC}"
    echo -e "  Hadoop版本: ${YELLOW}$HADOOP_VERSION${NC}"
    echo -e "  运行用户: ${YELLOW}$HADOOP_USER${NC}"

    echo -e "\n${CYAN}${BOLD}🔍 节点状态：${NC}"
    local alive_nodes=0
    local total_nodes=0

    for node in $ALL_NODES; do
        ((total_nodes++))
        if ping -c 1 -W 1 "$node" &> /dev/null; then
            ((alive_nodes++))
            echo -e "  ${GREEN}✓${NC} $node - 在线"
        else
            echo -e "  ${RED}✗${NC} $node - 离线"
        fi
    done

    echo -e "\n${CYAN}${BOLD}🔄 服务状态：${NC}"

    # 检查HDFS
    if command -v hdfs &> /dev/null; then
        if hdfs dfsadmin -report 2>/dev/null | grep -q "Live datanodes"; then
            local datanodes=$(hdfs dfsadmin -report 2>&1 | grep "Live datanodes" | awk '{print $3}')
            echo -e "  ${GREEN}✓${NC} HDFS - 运行中 (DataNodes: $datanodes)"
        else
            echo -e "  ${RED}✗${NC} HDFS - 未运行"
        fi
    else
        echo -e "  ${YELLOW}⚠${NC} HDFS - 命令不可用"
    fi

    # 检查YARN
    if command -v yarn &> /dev/null; then
        if yarn node -list 2>/dev/null | grep -q "Total Nodes"; then
            local yarn_nodes=$(yarn node -list 2>&1 | grep "Total Nodes" | awk '{print $3}')
            echo -e "  ${GREEN}✓${NC} YARN - 运行中 (Nodes: $yarn_nodes)"
        else
            echo -e "  ${RED}✗${NC} YARN - 未运行"
        fi
    else
        echo -e "  ${YELLOW}⚠${NC} YARN - 命令不可用"
    fi

    echo -e "\n${CYAN}${BOLD}🌐 Web UI访问：${NC}"
    echo -e "  NameNode: http://${NAMENODE_NODE}:9870"
    echo -e "  ResourceManager: http://${RESOURCEMANAGER_NODE}:8088"

    echo -e "\n${GREEN}${BOLD}══════════════════════════════════════════════════════════${NC}"
    echo -e "  在线节点: ${alive_nodes}/${total_nodes}"
    echo -e "  检查时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo -e "${GREEN}${BOLD}══════════════════════════════════════════════════════════${NC}"

    echo -e "\n${YELLOW}按任意键返回主菜单...${NC}"
    read -n1 -s
}

# 打印步骤信息
print_step() {
    #((STEP++))
    STEP=$((STEP + 1))
	local step_msg="$1"
    local description="$2"

    echo -e "\n${CYAN}${BOLD}[步骤 $STEP/$TOTAL_STEPS] ${NC}${step_msg}"
    echo -e "${DIM}${description}${NC}"
    echo -e "${BLUE}┌─────────────────────────────────────────────────────${NC}"
}

# 打印完成信息
print_step_complete() {
    echo -e "${BLUE}└─────────────────────────────────────────────────────${NC}"
}

# 打印成功信息
print_success() {
    local message="$1"
    echo -e "  ${GREEN}✓${NC} ${message}"
    #((SUCCESS_COUNT++))
	SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    echo "$(date +'%Y-%m-%d %H:%M:%S') [SUCCESS] $message" >> "$LOG_FILE"
}

# 打印警告信息
print_warning() {
    local message="$1"
    echo -e "  ${YELLOW}⚠${NC} ${message}"
    echo "$(date +'%Y-%m-%d %H:%M:%S') [WARNING] $message" >> "$LOG_FILE"
}

# 打印错误信息
print_error() {
    local message="$1"
    echo -e "  ${RED}✗${NC} ${message}"
    ERROR_COUNT=$((ERROR_COUNT + 1))
    echo "$(date +'%Y-%m-%d %H:%M:%S') [ERROR] $message" >> "$LOG_FILE"
}

# 打印信息
print_info() {
    local message="$1"
    echo -e "  ${BLUE}ℹ${NC} ${message}"
    echo "$(date +'%Y-%m-%d %H:%M:%S') [INFO] $message" >> "$LOG_FILE"
}

# 打印进度条
print_progress() {
    local current="$1"
    local total="$2"
    local width=50
    local percentage=$((current * 100 / total))
    local completed=$((width * current / total))
    local remaining=$((width - completed))

    printf "\r${BLUE}["
    printf "%${completed}s" | tr ' ' '='
    printf "%${remaining}s" | tr ' ' ' '
    printf "] ${percentage}%%${NC}"
}

# 优雅的Spinner
spinner() {
    local pid=$1
    local delay=0.1
    local spinstr='|/-\'

    while [ "$(ps a | awk '{print $1}' | grep $pid)" ]; do
        local temp=${spinstr#?}
        printf "  [%c] " "$spinstr"
        local spinstr=$temp${spinstr%"$temp"}
        sleep $delay
        printf "\b\b\b\b\b\b"
    done
    printf "    \b\b\b\b"
}

# ==================== 错误处理函数 ====================

# 错误处理函数
handle_error() {
    local exit_code=$1
    local line_no=$2
	local command=$3
    #local error_msg="脚本在行号 $line_no 处退出，退出码: $exit_code"

	# 如果是在清理过程中出错，直接退出
    if [[ "$command" == *cleanup_on_exit* ]]; then
        echo -e "${RED}清理过程中出错，强制退出${NC}"
        exit $exit_code
    fi

    echo -e "\n${RED}${BOLD}══════════════════════════════════════════════════════════${NC}"
    echo -e "${RED}${BOLD}  错误发生！${NC}"
	echo -e "${RED}退出码: $exit_code${NC}"
    echo -e "${RED}行号: $line_no${NC}"
    echo -e "${RED}命令: $command${NC}"
    #echo -e "${RED}错误信息: ${error_msg}${NC}"
    echo -e "${RED}详细日志请查看: $LOG_FILE${NC}"
    echo -e "${RED}${BOLD}══════════════════════════════════════════════════════════${NC}"

	# 显示日志文件的最后10行
    echo -e "\n${YELLOW}日志最后10行:${NC}"
    tail -10 "$LOG_FILE" 2>/dev/null || echo "无法读取日志文件"

#    # 询问是否继续
#    echo -e "\n${YELLOW}是否尝试继续执行？(y/n): ${NC}\c"
#    read -r continue_choice
#    if [[ "$continue_choice" =~ ^[Yy]$ ]]; then
#        return 0
#    else
#        cleanup_on_exit
#        exit $exit_code
#    fi

	# 直接退出，不询问
    cleanup_on_exit
    exit $exit_code
}

# 安全执行函数（用于需要继续执行的模块）
safe_execute() {
    local module_name="$1"
    local module_function="$2"

    #echo -e "\n${BLUE}执行模块: $module_name${NC}"

    # 临时禁用 ERR trap
    trap '' ERR

    # 执行模块
    if $module_function; then
        #echo -e "${GREEN}✓ 模块 $module_name 执行成功${NC}"
        trap 'handle_error $? $LINENO "$BASH_COMMAND"' ERR  # 重新启用 trap
        return 0
    else
        local exit_code=$?
        echo -e "${RED}✗ 模块 $module_name 执行失败 (退出码: $exit_code)${NC}"
        trap 'handle_error $? $LINENO "$BASH_COMMAND"' ERR  # 重新启用 trap
        return $exit_code
    fi
}


# 清理函数
cleanup_on_exit() {
    local end_time=$(date +%s)
    local duration=$((end_time - START_TIME))

    echo -e "\n${BLUE}${BOLD}══════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}${BOLD}  执行摘要${NC}"
    echo -e "${BLUE}${BOLD}══════════════════════════════════════════════════════════${NC}"
    echo -e "  总步骤: $TOTAL_STEPS"
    echo -e "  成功: ${GREEN}$SUCCESS_COUNT${NC}"
    echo -e "  错误: ${RED}$ERROR_COUNT${NC}"
    echo -e "  耗时: ${YELLOW}${duration}秒${NC}"
    echo -e "  日志文件: $LOG_FILE"
    echo -e "  配置备份: $CONFIG_FILE"
    echo -e "${BLUE}${BOLD}══════════════════════════════════════════════════════════${NC}"
}

# 配置验证函数
validate_config() {
    #print_step "验证配置" "检查所有必需的配置参数"

    # 检查必要参数
    local missing_params=()

    [ -z "$MASTER_NODE" ] && missing_params+=("MASTER_NODE")
    [ -z "$WORKER_NODES" ] && missing_params+=("WORKER_NODES")
    [ -z "$HADOOP_USER" ] && missing_params+=("HADOOP_USER")
    [ -z "$HADOOP_HOME" ] && missing_params+=("HADOOP_HOME")
    [ -z "$HADOOP_VERSION" ] && missing_params+=("HADOOP_VERSION")

    if [ ${#missing_params[@]} -gt 0 ]; then
        print_error "缺少必需的配置参数: ${missing_params[*]}"
        exit 1
    fi

    #print_success "配置验证通过"
    #print_step_complete
}


# ==================== 新增卸载模块 ====================

# 模块A：卸载确认
module_uninstall_confirm() {
    print_step "卸载确认" "确认卸载Hadoop集群操作"

    echo -e "${RED}${BOLD}══════════════════════════════════════════════════════════${NC}"
    echo -e "${RED}${BOLD}                ⚠️  警告：危险操作！                     ${NC}"
    echo -e "${RED}${BOLD}══════════════════════════════════════════════════════════${NC}"

    echo -e "\n${YELLOW}${BOLD}此操作将永久删除以下内容：${NC}"
    echo -e "  ${RED}• Hadoop安装目录: $HADOOP_HOME${NC}"
    echo -e "  ${RED}• 所有数据目录: $DATA_DIR${NC}"
    echo -e "  ${RED}• 日志和PID目录: $LOG_DIR, $PID_DIR${NC}"
    echo -e "  ${RED}• 所有配置文件和环境变量${NC}"
    echo -e "  ${RED}• 集群所有节点上的Hadoop相关文件${NC}"

    echo -e "\n${YELLOW}${BOLD}影响范围：${NC}"
    for node in $ALL_NODES; do
        echo -e "  ${CYAN}• $node${NC}"
    done

    # 确认选择
    echo -e "\n${YELLOW}${BOLD}请选择操作：${NC}"
    echo -e "  1) ${GREEN}取消卸载${NC} - 返回主菜单"
    echo -e "  2) ${YELLOW}普通卸载${NC} - 删除Hadoop文件，保留用户和目录"
    echo -e "  3) ${RED}完全卸载${NC} - 删除所有相关文件，包括用户"

    echo -e "\n${YELLOW}请输入选择 (1/2/3): ${NC}\c"
    read -r uninstall_choice

    case $uninstall_choice in
        1)
            print_info "卸载操作已取消"
            return 1
            ;;
        2)
            UNINSTALL_LEVEL="normal"
            print_info "选择普通卸载模式"
            ;;
        3)
            UNINSTALL_LEVEL="full"
            print_info "选择完全卸载模式"
            ;;
        *)
            print_error "无效选择，默认使用普通卸载"
            UNINSTALL_LEVEL="normal"
            ;;
    esac

    # 最终确认
    if [ "$CONFIRM_BEFORE_REMOVE" = true ]; then
        echo -e "\n${RED}${BOLD}最后一次确认！${NC}"
        echo -e "${RED}请输入 'YES, DELETE HADOOP' 以确认卸载：${NC}\c"
        read -r final_confirm

        if [ "$final_confirm" != "YES, DELETE HADOOP" ]; then
            print_info "卸载操作已取消"
            return 1
        fi
    fi

    print_success "确认完成，开始卸载流程"
    print_step_complete
    return 0
}

# 模块B：停止所有服务
module_stop_services() {
    print_step "停止服务" "停止所有Hadoop集群服务"

    # 停止服务的函数
    stop_hadoop_services() {
        print_info "停止当前节点的Hadoop服务..."

        # 停止JobHistory Server
        if pgrep -f "historyserver" > /dev/null; then
            sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$JOBHISTORY_NODE" bash -c '
			# 加载hadoop用户的环境
			if [ -f ~/.bashrc ]; then
				source ~/.bashrc
			fi
			if [ -f /etc/profile.d/hadoop.sh ]; then
				source /etc/profile.d/hadoop.sh
			fi

			# 启动JobHistory Server
			"$HADOOP_HOME/bin/mapred" --daemon stop historyserver
			' 2>/dev/null || true
            print_info "停止JobHistory Server"
        fi

        # 停止YARN
        if [ -f "$HADOOP_HOME/sbin/stop-yarn.sh" ]; then
            sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$RESOURCEMANAGER_NODE" bash -c '
			# 加载hadoop用户的环境
			if [ -f ~/.bashrc ]; then
				source ~/.bashrc
			fi
			if [ -f /etc/profile.d/hadoop.sh ]; then
				source /etc/profile.d/hadoop.sh
			fi

			"$HADOOP_HOME/sbin/stop-yarn.sh"
			' 2>/dev/null || true
            print_info "停止YARN服务"
        fi

        # 停止HDFS
        if [ -f "$HADOOP_HOME/sbin/stop-dfs.sh" ]; then
            sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$NAMENODE_NODE" bash -c '
			# 加载hadoop用户的环境
			if [ -f ~/.bashrc ]; then
				source ~/.bashrc
			fi
			if [ -f /etc/profile.d/hadoop.sh ]; then
				source /etc/profile.d/hadoop.sh
			fi

			"$HADOOP_HOME/sbin/stop-dfs.sh"
			' 2>/dev/null || true
            print_info "停止HDFS服务"
        fi

        # 停止所有节点上的服务
        for node in $ALL_NODES; do
            if [ "$node" != "$(hostname)" ]; then
                print_info "停止节点 $node 的服务..."

                sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" bash -c '

                    # 停止SecondaryNameNode
                    $HADOOP_HOME/bin/hdfs --daemon stop secondarynamenode 2>/dev/null || true

                    # 停止DataNode
                    $HADOOP_HOME/bin/hdfs --daemon stop datanode 2>/dev/null || true

                    # 停止NodeManager
                    $HADOOP_HOME/bin/yarn --daemon stop nodemanager 2>/dev/null || true

                    # 杀死残留进程
                    pkill -u $HADOOP_USER -f hadoop 2>/dev/null || true
                    pkill -u $HADOOP_USER -f yarn 2>/dev/null || true
                    pkill -u $HADOOP_USER -f hdfs 2>/dev/null || true
                ' 2>/dev/null || print_warning "节点 $node 服务停止时出现警告"
            fi
        done

        # 等待进程停止
        sleep 3

        # 强制终止残留进程
        for node in $ALL_NODES; do
            sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "pkill -9 -u $HADOOP_USER -f 'hadoop|yarn|hdfs'" 2>/dev/null || true 2>/dev/null
        done
    }

    # 安全执行停止服务
    if safe_execute "停止Hadoop服务" stop_hadoop_services; then
        print_success "所有Hadoop服务已停止"
    else
        print_warning "部分服务可能未完全停止"
    fi

    print_step_complete
}

# 模块C：删除文件和目录
module_remove_files() {
    print_step "删除文件" "删除Hadoop相关文件和目录"

	# 设置默认值（如果变量未定义）
    local BACKUP_BEFORE_REMOVE=${BACKUP_BEFORE_REMOVE:-false}

    # 创建备份（可选）
    if [ "$BACKUP_BEFORE_REMOVE" = true ]; then
        local backup_dir="/tmp/hadoop-backup-$(date +%Y%m%d-%H%M%S)"
        sudo mkdir -p "$backup_dir"

        print_info "创建配置文件备份: $backup_dir"
        sudo cp -r "$HADOOP_HOME/etc/hadoop" "$backup_dir/config" 2>/dev/null || true
        sudo cp -r "/etc/profile.d/hadoop.sh" "$backup_dir/" 2>/dev/null || true
    fi

    # 删除本地文件
    print_info "删除本地文件..."

    local items_to_remove=(
        "$HADOOP_HOME"
        "$DATA_DIR"
        "$LOG_DIR"
        "$PID_DIR"
        "/tmp/hadoop-*"
        "/tmp/hsperfdata_$HADOOP_USER"
        "/tmp/jetty_*"
    )

    for item in "${items_to_remove[@]}"; do
        if [ -e "$item" ] || [ "$item" == *"*"* ]; then
            sudo rm -rf $item 2>/dev/null || true
            print_info "删除: $item"
        fi
    done

    # 删除所有节点上的文件
    for node in $ALL_NODES; do
        print_info "清理节点: $node"

        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "
            set -e

            # 删除Hadoop目录
            if [ -d '$HADOOP_HOME' ]; then
                sudo rm -rf '$HADOOP_HOME'
                echo '删除Hadoop安装目录'
            fi

            # 删除数据目录
            if [ -d '$DATA_DIR' ]; then
                sudo rm -rf '$DATA_DIR'
                echo '删除数据目录'
            fi

            # 删除日志和PID目录
            if [ -d '$LOG_DIR' ]; then
                sudo rm -rf '$LOG_DIR'
                echo '删除日志目录'
            fi
            if [ -d '$PID_DIR' ]; then
                sudo rm -rf '$PID_DIR'
                echo '删除PID目录'
            fi

            # 清理临时文件
            sudo rm -rf /tmp/hadoop-* /tmp/hsperfdata_$HADOOP_USER /tmp/Jetty_* 2>/dev/null || true
            sudo rm -f /tmp/*.pid /tmp/*.out 2>/dev/null || true

            # 删除环境变量文件
            sudo rm -f /etc/profile.d/hadoop.sh 2>/dev/null || true
            sudo rm -f /etc/sudoers.d/hadoop-$HADOOP_USER 2>/dev/null || true

            # 清理.bashrc中的Hadoop配置
            sudo sed -i '/HADOOP_HOME/d' /home/$HADOOP_USER/.bashrc 2>/dev/null || true
            sudo sed -i '/JAVA_HOME/d' /home/$HADOOP_USER/.bashrc 2>/dev/null || true
            sudo sed -i '/HADOOP_CONF_DIR/d' /home/$HADOOP_USER/.bashrc 2>/dev/null || true
        " 2>/dev/null || print_warning "节点 $node 清理时出现警告"
    done

    # 删除本地的环境变量文件
    sudo rm -f /etc/profile.d/hadoop.sh 2>/dev/null || true
    sudo rm -f /etc/sudoers.d/hadoop-$HADOOP_USER 2>/dev/null || true

    print_success "文件和目录删除完成"
    print_step_complete
}

# 模块D：清理用户和组（仅完全卸载）
module_clean_users() {
    if [ "$UNINSTALL_LEVEL" != "full" ]; then
        print_info "跳过用户清理（普通卸载模式）"
        return 0
    fi

    print_step "清理用户" "删除Hadoop用户和组"

    for node in $ALL_NODES; do
        print_info "清理节点 $node 的用户..."

        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "
            set -e

            # 检查用户是否存在
            if id '$HADOOP_USER' &> /dev/null; then
                # 检查用户主目录是否为空
                if [ -d '/home/$HADOOP_USER' ]; then
                    file_count=\$(sudo find /home/$HADOOP_USER -maxdepth 1 -type f 2>/dev/null | wc -l)
                    dir_count=\$(sudo find /home/$HADOOP_USER -maxdepth 1 -type d 2>/dev/null | wc -l)

                    if [ \$file_count -eq 0 ] && [ \$dir_count -le 1 ]; then
                        # 主目录基本为空，可以删除用户
                        sudo userdel -r '$HADOOP_USER' 2>/dev/null || true
                        echo '删除用户: $HADOOP_USER'

                        # 删除用户组（如果没有其他成员）
                        if getent group '$HADOOP_GROUP' &> /dev/null; then
                            group_members=\$(getent group '$HADOOP_GROUP' | cut -d: -f4)
                            if [ -z \"\$group_members\" ]; then
                                sudo groupdel '$HADOOP_GROUP' 2>/dev/null || true
                                echo '删除组: $HADOOP_GROUP'
                            else
                                echo '组 $HADOOP_GROUP 仍有其他成员，保留'
                            fi
                        fi
                    else
                        echo '用户主目录非空，保留用户'
                        echo '您可以手动清理: sudo rm -rf /home/$HADOOP_USER'
                    fi
                fi
            else
                echo '用户 $HADOOP_USER 不存在'
            fi
        " 2>/dev/null || print_warning "节点 $node 用户清理时出现警告"
    done

    print_success "用户和组清理完成"
    print_step_complete
}

# 模块E：清理防火墙规则
module_clean_firewall() {
    print_step "清理防火墙" "删除Hadoop相关防火墙规则"

    # 需要开放的端口列表
    local ports=("9820" "9870" "9866" "9864" "9868" "8088" "8042" "19888" "10020")

    for node in $ALL_NODES; do
        print_info "清理节点 $node 的防火墙规则..."

        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "
            set -e

            # 检测防火墙类型
            if command -v ufw &> /dev/null; then
                # Ubuntu/Debian: ufw
                for port in ${ports[@]}; do
                    sudo ufw delete allow \"\$port/tcp\" 2>/dev/null || true
                done
                sudo ufw reload 2>/dev/null || true
                echo 'UFW防火墙规则已清理'

            elif command -v firewall-cmd &> /dev/null; then
                # CentOS/RHEL: firewalld
                for port in ${ports[@]}; do
                    sudo firewall-cmd --permanent --remove-port=\"\$port/tcp\" 2>/dev/null || true
                done
                sudo firewall-cmd --reload 2>/dev/null || true
                echo 'Firewalld防火墙规则已清理'

            elif command -v iptables &> /dev/null; then
                # 传统iptables
                echo '检测到iptables，防火墙规则需要手动清理'
                echo '相关端口: ${ports[@]}'
            else
                echo '未检测到防火墙工具'
            fi
        " 2>/dev/null || print_warning "节点 $node 防火墙清理时出现警告"
    done

    print_success "防火墙规则清理完成"
    print_step_complete
}

# 模块F：卸载完成验证
module_uninstall_verify() {
    print_step "验证卸载" "验证Hadoop集群已完全卸载"

	# 停止脚本自身的后台进程
    print_info "清理脚本相关进程..."

    # 杀死可能存在的僵尸ssh进程
    pkill -f "hadoop" 2>/dev/null || true

    local verification_passed=0
    local verification_total=0

    print_info "检查各节点卸载情况..."

    for node in $ALL_NODES; do
        ((verification_total++))

        # 检查Hadoop目录是否存在
        if sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "[ ! -d '$HADOOP_HOME' ]" 2>/dev/null; then
            ((verification_passed++))
            print_success "节点 $node: Hadoop目录已删除 ✓"
        else
            print_warning "节点 $node: Hadoop目录可能还存在"
        fi

        # 检查进程是否还在运行
        #local process_count=$(ssh "$HADOOP_USER@$node" "pgrep -u $HADOOP_USER -f 'hadoop|yarn|hdfs' 2>/dev/null | wc -l" 2>/dev/null || echo "0")
        local process_count=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "pgrep -f 'hadoop|yarn|hdfs' 2>/dev/null | wc -l" 2>/dev/null || echo "0")
        if [ "$process_count" -eq 0 ]; then
            print_success "节点 $node: 无Hadoop进程运行 ✓"
        else
            print_warning "节点 $node: 仍有 $process_count 个Hadoop进程在运行"
        fi
    done

    # 检查本地
    if [ ! -d "$HADOOP_HOME" ]; then
        ((verification_passed++))
        print_success "本地Hadoop目录已删除 ✓"
    else
        print_warning "本地Hadoop目录仍然存在"
    fi

	local BACKUP_BEFORE_REMOVE=${BACKUP_BEFORE_REMOVE:-false}

    if [ $verification_passed -eq $((verification_total + 1)) ]; then
        echo -e "\n${GREEN}${BOLD}══════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}${BOLD}      🎉 Hadoop集群卸载完成！                          ${NC}"
        echo -e "${GREEN}${BOLD}══════════════════════════════════════════════════════════${NC}"

        if [ "$BACKUP_BEFORE_REMOVE" = true ] && [ -d "$backup_dir" ]; then
            echo -e "${YELLOW}配置文件备份位于: $backup_dir${NC}"
            echo -e "${YELLOW}请在确认不再需要后手动删除备份${NC}"
        fi

        echo -e "\n${CYAN}${BOLD}📋 卸载完成总结：${NC}"
        echo -e "  删除级别: ${YELLOW}$UNINSTALL_LEVEL${NC}"
        echo -e "  清理节点数: ${YELLOW}${verification_total}${NC}"
        echo -e "  验证通过: ${GREEN}${verification_passed}/$((verification_total + 1))${NC}"

        if [ "$UNINSTALL_LEVEL" = "normal" ]; then
            echo -e "\n${BLUE}${BOLD}💡 注意：${NC}"
            echo -e "  Hadoop用户和组仍保留，便于重新安装"
            echo -e "  如需完全清理，请选择'完全卸载'模式"
        fi

        echo -e "\n${GREEN}现在您可以重新运行安装脚本创建新的集群。${NC}"
    else
        print_warning "卸载基本完成，但建议手动检查以下项目："
        echo -e "  1. 检查所有节点的 $HADOOP_HOME 目录"
        echo -e "  2. 检查所有节点的Hadoop相关进程"
        echo -e "  3. 检查环境变量配置"
        echo -e "  4. 检查防火墙规则"
    fi

    print_step_complete
}

# 显示卸载警告
show_uninstall_warning() {
    echo -e "${RED}${BOLD}══════════════════════════════════════════════════════════${NC}"
    echo -e "${RED}${BOLD}                ⚠️  警告：危险操作！                     ${NC}"
    echo -e "${RED}${BOLD}══════════════════════════════════════════════════════════${NC}"

    echo -e "\n${YELLOW}${BOLD}此操作将永久删除以下内容：${NC}"
    echo -e "  ${RED}• Hadoop安装目录: $HADOOP_HOME${NC}"
    echo -e "  ${RED}• 所有数据目录: $DATA_DIR${NC}"
    echo -e "  ${RED}• 日志和PID目录: $LOG_DIR, $PID_DIR${NC}"
    echo -e "  ${RED}• 所有配置文件和环境变量${NC}"
    echo -e "  ${RED}• 集群所有节点上的Hadoop相关文件${NC}"

    echo -e "\n${YELLOW}${BOLD}影响范围：${NC}"
    for node in $ALL_NODES; do
        echo -e "  ${CYAN}• $node${NC}"
    done
}

# 获取用户确认
get_uninstall_confirmation() {
    echo -e "\n${YELLOW}${BOLD}请选择操作：${NC}"
    echo -e "  1) ${GREEN}取消卸载${NC} - 返回主菜单"
    echo -e "  2) ${YELLOW}普通卸载${NC} - 删除Hadoop文件，保留用户和目录"
    echo -e "  3) ${RED}完全卸载${NC} - 删除所有相关文件，包括用户"

    echo -e "\n${YELLOW}请输入选择 (1/2/3): ${NC}\c"
    read -r uninstall_choice

    case $uninstall_choice in
        1)
            return 1
            ;;
        2)
            UNINSTALL_LEVEL="normal"
            echo -e "${YELLOW}选择普通卸载模式${NC}"
            ;;
        3)
            UNINSTALL_LEVEL="full"
            echo -e "${RED}选择完全卸载模式${NC}"
            ;;
        *)
            echo -e "${RED}无效选择，默认使用普通卸载${NC}"
            UNINSTALL_LEVEL="normal"
            ;;
    esac

     # 最终确认
    local confirm_before_remove=${CONFIRM_BEFORE_REMOVE:-true}
    if [ "$confirm_before_remove" = true ]; then
        echo -e "\n${RED}${BOLD}最后一次确认！${NC}"
        echo -e "${RED}请输入 'YES, DELETE HADOOP' 以确认卸载：${NC}\c"
        read -r final_confirm

        if [ "$final_confirm" != "YES, DELETE HADOOP" ]; then
            return 1
        fi
    fi

    return 0
}

# ==================== 新增卸载主函数 ====================
uninstall_hadoop_cluster() {
    echo -e "\n${BLUE}开始执行Hadoop集群卸载...${NC}"

    # 显示警告信息
    show_uninstall_warning

    # 获取用户确认
    if ! get_uninstall_confirmation; then
        echo -e "${GREEN}卸载已取消。${NC}"
        return 0
    fi

    echo -e "\n${DIM}详细日志将保存到: $LOG_FILE${NC}"
    echo -e "${BLUE}${BOLD}══════════════════════════════════════════════════════════${NC}"

    # 记录开始时间
    local start_time=$(date +%s)

    # 执行卸载流程
    (

		# 重置步骤计数器
        STEP=0
        TOTAL_STEPS=5

        # 停止所有服务
        module_stop_services

        # 删除文件和目录
        module_remove_files

        # 清理用户和组（仅完全卸载）
        module_clean_users

        # 清理防火墙规则
        module_clean_firewall

        # 卸载完成验证
        module_uninstall_verify

    ) 2>&1 | tee -a "$LOG_FILE" || {
        echo "警告：日志记录可能不完整，但卸载过程继续..." >&2
    }

	# 显示完成信息
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    echo -e "\n${GREEN}${BOLD}✅ 卸载完成！耗时: ${duration}秒${NC}"
}


# ==================== 核心功能模块 ====================

# 模块1：系统准备
module_system_prepare() {
    print_step "系统准备" "更新系统并安装基础依赖"

    # 检查操作系统
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$ID
        OS_VERSION=$VERSION_ID
        print_info "检测到操作系统: $NAME $VERSION"
    else
        print_warning "无法检测操作系统，假设为Ubuntu/Debian"
        OS="ubuntu"
    fi

    # 安装基础依赖（包括sshpass）
    case $OS in
        ubuntu|debian)
            sudo apt-get update && sudo apt-get upgrade -y
            sudo apt-get install -y ssh pdsh curl wget tar gnupg lsb-release \
                net-tools dnsutils tree htop iotop iftop vim jq python3 python3-pip \
                sshpass expect
            ;;
        centos|rhel|fedora)
            sudo yum update -y
            sudo yum install -y epel-release
            sudo yum install -y ssh pdsh curl wget tar gnupg2 redhat-lsb-core \
                net-tools bind-utils tree htop iotop iftop vim jq python3 python3-pip \
                sshpass expect
            ;;
        *)
            print_warning "不支持的操作系统，跳过系统更新"
            ;;
    esac

    print_success "系统准备完成"
    print_step_complete
}

# 模块2：用户和组管理（仅管理用户，不创建Hadoop目录）
module_user_setup() {
    print_step "用户设置" "在所有节点创建Hadoop专用用户和组"

    # 定义要同步的用户配置
    local hadoop_uid=1001  # 固定UID
    local hadoop_gid=1001  # 固定GID
    local hadoop_home="/home/$HADOOP_USER"
    local hadoop_shell="/bin/bash"
	local current_host=$(hostname)  # 获取当前主机名

    print_info "正在所有节点创建用户和组..."

	 # 声明并初始化 count 变量
    local count=0

    for node in $ALL_NODES; do
        print_info "配置节点: $node"

        # 在节点上执行用户创建
        ssh "$node" "bash -c '
        set -e

        # 创建用户组（固定GID）
        if getent group \"$HADOOP_GROUP\" > /dev/null; then
            #echo \"用户组 $HADOOP_GROUP 已存在\"
			:
        else
            sudo groupadd -g $hadoop_gid \"$HADOOP_GROUP\"
            #echo \"创建用户组: $HADOOP_GROUP (GID:$hadoop_gid)\"
        fi

        # 创建用户（固定UID）
        if id \"$HADOOP_USER\" &> /dev/null; then
            #echo \"用户 $HADOOP_USER 已存在\"
            # 检查UID是否匹配
            current_uid=\$(id -u \"$HADOOP_USER\")
            if [ \"\$current_uid\" != \"$hadoop_uid\" ]; then
                echo \"警告: 用户 $HADOOP_USER 的UID(\$current_uid)与配置($hadoop_uid)不匹配\"
            fi
        else
            sudo useradd -m -u $hadoop_uid -g $hadoop_gid \
                -s \"$hadoop_shell\" -d \"$hadoop_home\" \"$HADOOP_USER\"
            #echo \"创建用户: $HADOOP_USER (UID:$hadoop_uid)\"
            #echo \"$HADOOP_USER:$HADOOP_PASSWORD\" | sudo chpasswd
        fi

        # 确保用户主目录存在并有正确权限
        sudo mkdir -p \"$hadoop_home\"
        sudo chown -R \"$HADOOP_USER:$HADOOP_GROUP\" \"$hadoop_home\"
        sudo chmod 755 \"$hadoop_home\"

        # 创建.ssh目录用于后续SSH配置
        sudo mkdir -p \"$hadoop_home/.ssh\"
        sudo chown -R \"$HADOOP_USER:$HADOOP_GROUP\" \"$hadoop_home/.ssh\"
        sudo chmod 700 \"$hadoop_home/.ssh\"

        # 配置sudo权限（无密码）
        sudo_file=\"/etc/sudoers.d/hadoop-$HADOOP_USER\"

		# 备份旧文件（如果存在）
        if [ -f \"\$sudo_file\" ]; then
            sudo cp \"\$sudo_file\" \"\$sudo_file.backup-$(date +%Y%m%d%H%M%S)\"
            #echo \"备份旧sudoers文件: \$sudo_file -> \$sudo_file.backup\"
        fi

		# 首先检查sudo环境下的yum路径
        #echo \"检查sudo环境下的yum路径...\"
        sudo_yum_path=\$(sudo which yum 2>/dev/null || echo \"/bin/yum\")
        #echo \"sudo环境下的yum路径: \$sudo_yum_path\"

		# 检查/bin/yum是否存在（可能是一个符号链接）
        if [ -L \"/bin/yum\" ]; then
            yum_target=\$(readlink -f \"/bin/yum\")
            #echo \"/bin/yum 是指向 \$yum_target 的符号链接\"
        fi

		# 创建新的sudoers文件 - 使用sudo环境下的路径
        # 注意：sudoers中不允许命令包含通配符参数，所以需要简化
        sudo tee \"\$sudo_file\" > /dev/null << 'EOF'
	$HADOOP_USER ALL=(ALL) NOPASSWD:\
	\$sudo_yum_path,\
    /usr/bin/tee,\
    /usr/bin/yum,\
    /usr/bin/systemctl,\
    /usr/bin/pkill,\
    /usr/bin/pgrep,\
    /opt/module/hadoop*/sbin/*,\
    /opt/module/hadoop*/bin/*,\
    /bin/cp,\
    /bin/chown,\
    /bin/chmod,\
    /bin/mkdir,\
    /bin/rm,\
    /bin/mv,\
    /bin/cat,\
    /bin/grep,\
    /bin/pgrep,\
    /bin/sed,\
    /bin/bash,\
    /usr/sbin/useradd,\
    /usr/sbin/groupadd,\
	/usr/sbin/service,\
    /bin/passwd
EOF
        # 设置文件权限并验证语法
        sudo chmod 440 \"\$sudo_file\"
        #echo \"配置sudo权限\"

        # 验证sudoers文件语法
        if sudo visudo -c -f \"\$sudo_file\" 2>/dev/null; then
            #echo \"sudoers文件语法正确，权限已更新\"

			# 显示配置的权限（使用更简单的方法）
			#echo \"当前配置的sudo权限如下：\"
			#sudo cat \"\$sudo_file\"
			:
        else
            #echo \"错误：sudoers文件语法有问题，恢复备份\"
            if ls \"\$sudo_file.backup-\"* 2>/dev/null | head -1; then
                latest_backup=\$(ls -t \"\$sudo_file.backup-\"* | head -1)
                sudo mv \"\$latest_backup\" \"\$sudo_file\"
                #echo \"已恢复备份: \$latest_backup\"
            else
                sudo rm -f \"\$sudo_file\"
                #echo \"已删除错误的sudoers文件\"
            fi
            exit 1
        fi

        # 显示hadoop用户的sudo权限
        #echo \"测试hadoop用户的sudo权限：\"
        sudo -lU \"$HADOOP_USER\" 2>/dev/null | tail -2 || echo \"无法显示sudo权限，但文件已创建\"

        #echo \"节点 $node 用户配置完成\"
        '" &

        # 控制并发，避免同时创建太多用户
        ((count++))
        if [ $((count % 3)) -eq 0 ]; then
            wait
        fi
    done

    wait  # 等待所有后台任务完成

    # 验证所有节点的用户配置
    print_info "验证所有节点的用户配置..."
    local verification_passed=0
    local verification_total=0

    for node in $ALL_NODES; do
        ((verification_total++))

		# 区分本地节点和远程节点
        if [ "$node" = "$current_host" ]; then
            # 本地节点：直接使用本地命令验证
            if id -u "$HADOOP_USER" &> /dev/null; then
                local remote_uid=$(id -u "$HADOOP_USER")
                local remote_gid=$(id -g "$HADOOP_USER")

                if [ "$remote_uid" = "$hadoop_uid" ] && [ "$remote_gid" = "$hadoop_gid" ]; then
                    ((verification_passed++))
                    print_success "节点 $node: UID=$remote_uid, GID=$remote_gid ✓"
                else
                    print_warning "节点 $node: UID/GID不匹配 (UID=$remote_uid, GID=$remote_gid)"
                fi
            else
                print_error "节点 $node: 用户 $HADOOP_USER 不存在"
            fi
        else
            # 远程节点：使用ssh验证（使用当前用户而非hadoop用户）
            if ssh "$node" "id -u $HADOOP_USER" &> /dev/null; then
                local remote_uid=$(ssh "$node" "id -u $HADOOP_USER")
                local remote_gid=$(ssh "$node" "id -g $HADOOP_USER")

                if [ "$remote_uid" = "$hadoop_uid" ] && [ "$remote_gid" = "$hadoop_gid" ]; then
                    ((verification_passed++))
                    print_success "节点 $node: UID=$remote_uid, GID=$remote_gid ✓"
                else
                    print_warning "节点 $node: UID/GID不匹配 (UID=$remote_uid, GID=$remote_gid)"
                fi
            else
                print_error "节点 $node: 用户 $HADOOP_USER 不存在"
            fi
        fi
    done

    if [ $verification_passed -eq $verification_total ]; then
        print_success "所有节点用户配置验证通过 ($verification_passed/$verification_total)"
    else
        print_warning "部分节点用户配置需要检查 ($verification_passed/$verification_total)"
    fi

    print_step_complete
}


# 模块3：SSH密钥配置
module_ssh_setup() {
    print_step "SSH配置" "设置SSH免密登录"

	# 获取当前主机名
    local current_host=$(hostname)

    # 生成SSH密钥（如果不存在）
    local ssh_dir="/home/$HADOOP_USER/.ssh"
    sudo mkdir -p "$ssh_dir"
    sudo chown -R "$HADOOP_USER:$HADOOP_GROUP" "$ssh_dir"
    sudo chmod 700 "$ssh_dir"

	# 生成密钥（如果不存在）
    if [ ! -f "$ssh_dir/id_rsa" ]; then
        echo -e "\n${YELLOW}生成SSH密钥，按Enter键接受默认设置...${NC}"
        sudo -u "$HADOOP_USER" ssh-keygen -t rsa -P '' -f "$ssh_dir/id_rsa" -q
        print_success "SSH密钥已生成"
    else
        print_info "SSH密钥已存在"
    fi

    # 创建authorized_keys文件并添加公钥
    sudo -u "$HADOOP_USER" cat "$ssh_dir/id_rsa.pub" >> "$ssh_dir/authorized_keys"
    sudo chmod 600 "$ssh_dir/authorized_keys"

    # 配置SSH config文件，禁用严格主机密钥检查
    sudo -u "$HADOOP_USER" cat > "$ssh_dir/config" << EOF
Host *
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    LogLevel ERROR
    ConnectTimeout 30
    ServerAliveInterval 60
    ServerAliveCountMax 3
EOF
    sudo chmod 600 "$ssh_dir/config"

	# 第二步：使用expect脚本自动处理密码登录到其他节点
    print_info "开始配置集群SSH免密登录..."

	 # 定义配置节点的函数（不使用expect）
    configure_node_ssh_simple() {
        local node="$1"
        local password="$2"

        if [ "$node" = "$current_host" ]; then
            return 0  # 跳过当前节点
        fi

        print_info "配置节点: $node"

        # 1. 测试连接并接受主机密钥
        echo "首次连接，接受主机密钥..."
        sshpass -p "$password" ssh -o StrictHostKeyChecking=no "$HADOOP_USER@$node" "exit" 2>/dev/null || true

        # 2. 确保远程.ssh目录存在
        sshpass -p "$password" ssh "$HADOOP_USER@$node" \
            "mkdir -p ~/.ssh && chmod 700 ~/.ssh" 2>/dev/null

        # 3. 生成远程节点的密钥（如果不存在）
        sshpass -p "$password" ssh "$HADOOP_USER@$node" \
            "if [ ! -f ~/.ssh/id_rsa ]; then ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa -q; fi" 2>/dev/null

        # 4. 获取远程节点的公钥
        local remote_pubkey=$(sshpass -p "$password" ssh "$HADOOP_USER@$node" \
            "cat ~/.ssh/id_rsa.pub 2>/dev/null" 2>/dev/null)

        if [ -n "$remote_pubkey" ]; then
            # 将远程节点的公钥添加到本地的authorized_keys
            echo "$remote_pubkey" | sudo -u "$HADOOP_USER" tee -a "$ssh_dir/authorized_keys" > /dev/null
            print_success "节点 $node 公钥已收集"
        fi

        # 5. 将本地公钥复制到远程节点
        local local_pubkey=$(sudo -u "$HADOOP_USER" cat "$ssh_dir/id_rsa.pub")

        # 将本地公钥添加到远程节点的authorized_keys
        sshpass -p "$password" ssh "$HADOOP_USER@$node" "
            # 备份现有authorized_keys
            if [ -f ~/.ssh/authorized_keys ]; then
                cp ~/.ssh/authorized_keys ~/.ssh/authorized_keys.backup
            fi

            # 添加主节点的公钥
            echo '$local_pubkey' > ~/.ssh/authorized_keys

            # 添加自己的公钥（如果不在文件中）
            if [ -f ~/.ssh/id_rsa.pub ]; then
                cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
            fi

            # 设置权限
            chmod 600 ~/.ssh/authorized_keys

            # 创建config文件
            cat > ~/.ssh/config << 'CONFIGEOF'
Host *
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    LogLevel ERROR
    ConnectTimeout 30
    ServerAliveInterval 60
    ServerAliveCountMax 3
CONFIGEOF
            chmod 600 ~/.ssh/config

            echo 'SSH配置完成'
        " 2>/dev/null

        return 0
    }


	# 第三步：逐个配置其他节点
    local failed_nodes=()

	for node in $ALL_NODES; do
        if [ "$node" != "$current_host" ]; then
            if configure_node_ssh_simple "$node" "$HADOOP_PASSWORD"; then
                print_success "节点 $node SSH配置成功"
            else
                print_warning "节点 $node SSH自动配置失败"
                failed_nodes+=("$node")
            fi
        fi
    done

	 # 第四步：将当前节点收集的所有公钥合并并分发到所有节点
    print_info "合并并分发公钥到所有节点..."

    # 创建合并的公钥文件
    local merged_keys_file="/tmp/merged_keys_$(date +%s)"
    sudo -u "$HADOOP_USER" cat "$ssh_dir/authorized_keys" | sudo -u "$HADOOP_USER" sort -u > "$merged_keys_file"


	# 分发到所有节点
    for node in $ALL_NODES; do
        if [ "$node" != "$current_host" ]; then
            print_info "同步公钥到节点: $node"

            if sshpass -p "$HADOOP_PASSWORD" scp -o StrictHostKeyChecking=no \
                "$merged_keys_file" "$HADOOP_USER@$node:/tmp/merged_keys" 2>/dev/null; then

                sshpass -p "$HADOOP_PASSWORD" ssh "$HADOOP_USER@$node" "
                    # 使用合并的公钥文件
                    cat /tmp/merged_keys > ~/.ssh/authorized_keys
                    chmod 600 ~/.ssh/authorized_keys
                    rm -f /tmp/merged_keys
                " 2>/dev/null

                print_success "节点 $node 公钥已同步"
            else
                print_warning "节点 $node 公钥同步失败"
            fi
        fi
    done

	# 清理临时文件
    rm -f "$merged_keys_file" 2>/dev/null

    # 第五步：测试SSH免密登录
    print_info "测试SSH免密登录..."

    local success_count=0
    local total_tests=0

    echo -e "\n${CYAN}${BOLD}🔗 SSH连接测试：${NC}"

    # 测试从当前节点到所有其他节点的连接
    for node in $ALL_NODES; do
        if [ "$node" != "$current_host" ]; then
            ((total_tests++))

            if sudo -u "$HADOOP_USER" ssh -o ConnectTimeout=5 -o BatchMode=yes \
                "$node" "echo '从 $current_host 到 $node 连接成功'" 2>/dev/null; then
                ((success_count++))
                echo -e "  ${GREEN}$current_host -> $node: ✓${NC}"
            else
                echo -e "  ${RED}$current_host -> $node: ✗${NC}"
            fi
        fi
    done

    # 测试其他节点之间的连接（通过当前节点跳转）
    if [ -n "$WORKER_NODES" ]; then
        for src_node in $WORKER_NODES; do
            for dst_node in $ALL_NODES; do
                if [ "$src_node" != "$dst_node" ] && [ "$src_node" != "$current_host" ]; then
                    ((total_tests++))

                    # 从工作节点ssh到其他节点
                    if sudo -u "$HADOOP_USER" ssh -o ConnectTimeout=5 -o BatchMode=yes \
                        "$src_node" "ssh -o ConnectTimeout=5 -o BatchMode=yes '$dst_node' 'echo 1'" 2>/dev/null; then
                        ((success_count++))
                        echo -e "  ${GREEN}$src_node -> $dst_node: ✓${NC}"
                    else
                        echo -e "  ${RED}$src_node -> $dst_node: ✗${NC}"
                    fi
                fi
            done
        done
    fi

    echo -e "\n${CYAN}${BOLD}📊 测试结果统计：${NC}"
    echo -e "  总连接数: $total_tests"
    echo -e "  成功连接: ${GREEN}$success_count${NC}"
    echo -e "  失败连接: ${RED}$((total_tests - success_count))${NC}"

    if [ $success_count -eq $total_tests ]; then
        print_success "所有节点间SSH免密登录配置成功！"
    else
        print_warning "部分节点间SSH免密登录需要进一步配置"

        # 提供简化版的手动配置指南
        if [ ${#failed_nodes[@]} -gt 0 ]; then
            echo -e "\n${YELLOW}${BOLD}🔧 需要手动配置的节点：${NC}"
            for node in "${failed_nodes[@]}"; do
                echo -e "  ${RED}• $node${NC}"
            done

            echo -e "\n${YELLOW}${BOLD}💡 手动配置步骤：${NC}"
            echo -e "1. 在主节点($current_host)执行："
            echo -e "   sudo -u $HADOOP_USER ssh-copy-id $HADOOP_USER@目标节点"
            echo -e "   或者"
            echo -e "   sudo -u $HADOOP_USER cat ~/.ssh/id_rsa.pub | ssh $HADOOP_USER@目标节点 'cat >> ~/.ssh/authorized_keys'"

            echo -e "\n2. 在目标节点执行："
            echo -e "   chmod 600 ~/.ssh/authorized_keys"
            echo -e "   chmod 700 ~/.ssh"
        fi
    fi

    print_step_complete
}

# 模块4：Java安装
module_java_install() {

    print_step "Java安装" "在所有集群节点安装Java $JAVA_VERSION 运行环境"

	local current_host=$(hostname)
    local installed_count=0
    local verified_count=0
    local total_nodes=0

	# 根据操作系统设置默认路径
    local default_java_home=""
  # 根据操作系统设置Java安装命令
    local install_cmd=""
    case $OS in
        ubuntu|debian)
            install_cmd="sudo apt-get update -y && sudo apt-get install -y openjdk-${JAVA_VERSION}-jdk"
            default_java_home="/usr/lib/jvm/java-${JAVA_VERSION}-openjdk-amd64"
            ;;
        centos|rhel|fedora|rocky)
            install_cmd="sudo yum install -y java-${JAVA_VERSION}-openjdk-devel"
            default_java_home="/usr/lib/jvm/java-${JAVA_VERSION}-openjdk"
            ;;
        *)
            install_cmd="sudo yum install -y java-${JAVA_VERSION}-openjdk-devel"
            default_java_home="/usr/lib/jvm/java-${JAVA_VERSION}-openjdk"
            ;;
    esac

    print_info "Java安装命令: $install_cmd"

	# 安装Java的函数
    install_java_on_node() {
        local node="$1"
        local node_os="$2"

        print_info "在节点 $node 上安装Java $JAVA_VERSION..."

        # 根据操作系统调整命令
        local node_install_cmd=""
        case $node_os in
            ubuntu|debian)
                node_install_cmd="sudo apt-get update -y && sudo DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-${JAVA_VERSION}-jdk"
                ;;
            *)
                node_install_cmd="sudo yum install -y java-${JAVA_VERSION}-openjdk-devel"
                ;;
        esac

        # 执行安装
        if sudo -u "$HADOOP_USER" ssh -tt "$HADOOP_USER@$node" "bash -c '
            # 检查是否已安装合适版本的Java
            if command -v java &> /dev/null; then
                current_version=\"\$(java -version 2>&1 | head -1 | cut -d\\\" -f2)\"
                if [[ \"\$current_version\" == \"$JAVA_VERSION\"* ]]; then
                    echo \"Java \$current_version 已安装\"
                    exit 0
                else
                    echo \"当前Java版本: \$current_version，需要安装$JAVA_VERSION\"
                    # 卸载旧版本（可选）
                    # sudo yum remove -y java-* 2>/dev/null || true
                fi
            fi

            echo \"开始安装Java $JAVA_VERSION...\"
            $node_install_cmd

            if command -v java &> /dev/null; then
                installed_version=\"\$(java -version 2>&1 | head -1 | cut -d\\\" -f2)\"
                echo \"安装成功: Java \$installed_version\"
                exit 0
            else
                echo \"安装失败，Java命令未找到\"
                exit 1
            fi
        '" 2>&1; then
            return 0
        else
            return 1
        fi
    }

    # 检测节点操作系统
    detect_node_os() {
        local node="$1"
		sudo -u $HADOOP_USER ssh "$HADOOP_USER@$node" "cat /etc/os-release 2>/dev/null | grep '^ID=' | cut -d= -f2 | tr -d '\"'" 2>/dev/null || echo "centos"
    }

    # 处理每个节点
    for node in $ALL_NODES; do
        ((total_nodes++))
        print_info "处理节点: $node"

        local node_os=$(detect_node_os "$node")
        print_info "节点 $node 操作系统: $node_os"

        # 检查是否已安装合适版本的Java
        local java_check_result=$(sudo -u $HADOOP_USER ssh "$HADOOP_USER@$node" "command -v java && java -version 2>&1 | head -1" 2>/dev/null)

        if [ $? -eq 0 ] && [[ "$java_check_result" == *"$JAVA_VERSION"* ]]; then
            ((installed_count++))
            print_success "节点 $node: Java $JAVA_VERSION 已安装"
        else
            # 尝试安装
            if install_java_on_node "$node" "$node_os"; then
                ((installed_count++))
                print_success "节点 $node: Java $JAVA_VERSION 安装成功"
            else
                print_error "节点 $node: Java 安装失败"

                # 提供手动安装命令
                echo -e "${YELLOW}手动安装命令:${NC}"
                case $node_os in
                    ubuntu|debian)
                        echo -e " sudo -u $HADOOP_USER ssh $HADOOP_USER@$node 'sudo apt-get update && sudo apt-get install -y openjdk-${JAVA_VERSION}-jdk'"
                        ;;
                    *)
                        echo -e " sudo -u $HADOOP_USER ssh $HADOOP_USER@$node 'sudo yum install -y java-${JAVA_VERSION}-openjdk-devel'"
                        ;;
                esac
            fi
        fi

        # 验证安装
        if sudo -u $HADOOP_USER ssh "$HADOOP_USER@$node" "command -v java &> /dev/null" 2>/dev/null; then
            local version=$(sudo -u $HADOOP_USER ssh "$HADOOP_USER@$node" "java -version 2>&1 | head -1 | cut -d'\"' -f2" 2>/dev/null)
            if [[ "$version" == *"$JAVA_VERSION"* ]]; then
                ((verified_count++))
                print_success "节点 $node: Java验证通过 ($version)"
            else
                print_warning "节点 $node: Java版本不匹配 ($version)"
            fi
        fi
    done

    # 显示安装统计
    echo -e "\n${CYAN}${BOLD}📊 Java安装统计：${NC}"
    echo -e "  总节点数: ${total_nodes}"
    echo -e "  安装成功: ${GREEN}${installed_count}/${total_nodes}${NC}"
    echo -e "  验证通过: ${GREEN}${verified_count}/${total_nodes}${NC}"

    if [ $verified_count -eq $total_nodes ]; then
        print_success "所有节点Java安装验证通过！"
    else
        print_warning "Java安装不完整，请检查失败节点"

        # 列出失败的节点
        echo -e "\n${YELLOW}需要手动检查的节点：${NC}"
        for node in $ALL_NODES; do
            if ! sudo -u $HADOOP_USER ssh "$HADOOP_USER@$node" "command -v java &> /dev/null" 2>/dev/null; then
                echo -e "  ${RED}• $node${NC}"
            fi
        done
    fi

    print_step_complete
}

# 模块5：目录结构创建
module_directory_setup() {
    print_step "目录设置" "创建Hadoop所需的目录结构"

    # 定义目录列表
    local directories=(
        "$HADOOP_HOME"
        "$DATA_DIR/hdfs/name"
        "$DATA_DIR/hdfs/data"
        "$DATA_DIR/yarn/local"
        "$DATA_DIR/yarn/logs"
        "$DATA_DIR/tmp"
        "$LOG_DIR"
        "$PID_DIR"
        "/tmp/hadoop"
    )

    # 创建目录并设置权限
    for dir in "${directories[@]}"; do
        if [ ! -d "$dir" ]; then
            sudo mkdir -p "$dir"
            print_info "创建目录: $dir"
        fi

        # 设置所有者
        sudo chown -R "$HADOOP_USER:$HADOOP_GROUP" "$dir"

        # 设置权限
        if [[ "$dir" == */log* ]] || [[ "$dir" == */run* ]] || [[ "$dir" == */tmp* ]]; then
            sudo chmod -R 755 "$dir"
        else
            sudo chmod -R 750 "$dir"
        fi
    done

    # 设置setgid权限
    sudo chmod g+s "$HADOOP_HOME"
    sudo chmod g+s "$DATA_DIR"

    print_success "目录结构创建完成"
    print_step_complete
}

# 模块6：Hadoop下载和安装
module_hadoop_install() {
    print_step "Hadoop安装" "下载并安装Hadoop $HADOOP_VERSION"

    local hadoop_tar="hadoop-$HADOOP_VERSION.tar.gz"
    local hadoop_url="$HADOOP_MIRROR/hadoop-$HADOOP_VERSION/$hadoop_tar"
    local download_dir="/tmp"

    # 检查是否已下载
    if [ ! -f "$download_dir/$hadoop_tar" ]; then
        print_info "下载Hadoop: $HADOOP_VERSION"

        # 使用wget或curl下载
        if command -v wget &> /dev/null; then
            if ! sudo wget -q "$hadoop_url" -P "$download_dir"; then
                print_error "下载失败，请检查网络连接或镜像地址"
                return 1
            fi
        elif command -v curl &> /dev/null; then
             if ! sudo curl -sSL "$hadoop_url" -o "$download_dir/$hadoop_tar"; then
                print_error "下载失败，请检查网络连接或镜像地址"
                return 1
            fi
        else
            print_error "没有找到wget或curl，无法下载Hadoop"
            return 1
        fi

        if [ $? -eq 0 ]; then
            print_success "下载完成"
        else
            print_error "下载失败，请检查网络连接或镜像地址"
            return 1
        fi
    else
        print_info "使用已下载的Hadoop包"
    fi

	# 检查文件完整性
    if [ ! -s "$download_dir/$hadoop_tar" ]; then
        print_error "Hadoop包文件大小为0，可能下载不完整"
        return 1
    fi

    # 备份现有安装
    if [ -d "$HADOOP_HOME" ] && [ -d "$HADOOP_HOME/bin" ]; then
        local backup_dir="$HADOOP_HOME-backup-$(date +%Y%m%d-%H%M%S)"
        sudo mv "$HADOOP_HOME" "$backup_dir"
        print_info "备份现有安装到: $backup_dir"
    fi

	# 创建目标目录
    sudo mkdir -p "$(dirname $HADOOP_HOME)"

    # 解压安装
     if ! sudo tar -xzf "$download_dir/$hadoop_tar" -C "$(dirname $HADOOP_HOME)"; then
        print_error "解压Hadoop包失败，文件可能损坏"
        return 1
    fi

	 # 检查解压后的目录
    if [ ! -d "$(dirname $HADOOP_HOME)/hadoop-$HADOOP_VERSION" ]; then
        print_error "解压后未找到 hadoop-$HADOOP_VERSION 目录"
        return 1
    fi

	 # 移动目录
    if [ -d "$HADOOP_HOME" ]; then
        sudo rm -rf "$HADOOP_HOME"
    fi
    sudo mv "$(dirname $HADOOP_HOME)/hadoop-$HADOOP_VERSION" "$HADOOP_HOME"

    # 设置所有者
    sudo chown -R "$HADOOP_USER:$HADOOP_GROUP" "$HADOOP_HOME"

	# 验证安装（文件存在性）
    if [ -f "$HADOOP_HOME/bin/hadoop" ] && [ -d "$HADOOP_HOME/etc/hadoop" ]; then
        print_success "Hadoop安装成功: $HADOOP_HOME"

        # 尝试验证版本，但不强制要求
        if command -v java &> /dev/null; then
            # 检测Java安装路径
            local java_home_for_test=$(dirname $(dirname $(readlink -f $(which java))))

            if [ -d "$java_home_for_test" ]; then
                # 使用检测到的JAVA_HOME运行hadoop version
                local version_output=$(sudo -u "$HADOOP_USER" env JAVA_HOME="$java_home_for_test" "$HADOOP_HOME/bin/hadoop" version 2>&1 | head -2)

                if echo "$version_output" | grep -q "Hadoop"; then
                    print_info "$version_output"
                else
                    print_warning "版本检查失败，将在环境变量设置后重试"
                    print_info "已成功安装Hadoop $HADOOP_VERSION"
                fi
            else
                print_info "Hadoop $HADOOP_VERSION 已成功安装"
                print_info "Java环境将在后续步骤中配置"
            fi
        else
            print_info "Hadoop $HADOOP_VERSION 已成功安装"
            print_info "注意：Java未安装或未找到，将在后续步骤中处理"
        fi
    else
        print_error "Hadoop安装失败"
        return 1
    fi

    print_step_complete
	return 0
}

# 模块7：配置模板生成
module_config_templates() {
    print_step "配置模板" "生成Hadoop配置文件模板"

    # 创建配置目录
    local conf_dir="$HADOOP_HOME/etc/hadoop"

    # 生成hadoop-env.sh
    cat > /tmp/hadoop-env.sh.template << 'EOF'
#!/usr/bin/env bash

# JAVA_HOME
export JAVA_HOME=${JAVA_HOME}
# Hadoop配置目录
export HADOOP_CONF_DIR=${HADOOP_CONF_DIR}

# Hadoop日志目录
export HADOOP_LOG_DIR=${HADOOP_LOG_DIR}

# Hadoop PID目录
export HADOOP_PID_DIR=${HADOOP_PID_DIR}

# Hadoop堆内存设置
export HADOOP_HEAPSIZE_MAX=1024
export HADOOP_HEAPSIZE=1024

# Hadoop 3.x 服务用户配置（必须）
export HDFS_NAMENODE_USER=${HADOOP_USER}
export HDFS_DATANODE_USER=${HADOOP_USER}
export HDFS_SECONDARYNAMENODE_USER=${HADOOP_USER}
export YARN_RESOURCEMANAGER_USER=${HADOOP_USER}
export YARN_NODEMANAGER_USER=${HADOOP_USER}
export HDFS_JOURNALNODE_USER=${HADOOP_USER}
export HDFS_ZKFC_USER=${HADOOP_USER}
export MAPRED_HISTORYSERVER_USER=${HADOOP_USER}

# 垃圾回收优化
export HADOOP_OPTS="$HADOOP_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
export HADOOP_OPTS="$HADOOP_OPTS -XX:+UnlockExperimentalVMOptions"
export HADOOP_OPTS="$HADOOP_OPTS -XX:+UseContainerSupport"

# 网络优化
export HADOOP_CLIENT_OPTS="$HADOOP_CLIENT_OPTS -Djava.net.preferIPv4Stack=true"
EOF

    # 生成core-site.xml模板
    cat > /tmp/core-site.xml.template << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <!-- HDFS 默认文件系统地址 -->
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://${NAMENODE_NODE}:9820</value>
        <description>NameNode RPC地址，客户端通过此地址连接HDFS</description>
    </property>

    <!-- Hadoop 临时目录 -->
    <property>
        <name>hadoop.tmp.dir</name>
        <value>${DATA_DIR}/tmp</value>
        <description>Hadoop临时文件目录</description>
    </property>

    <!-- I/O 缓冲区大小 -->
    <property>
        <name>io.file.buffer.size</name>
        <value>131072</value>
        <description>读写操作的缓冲区大小</description>
    </property>

    <!-- WebHDFS 启用 -->
    <property>
        <name>dfs.webhdfs.enabled</name>
        <value>true</value>
        <description>启用WebHDFS REST API</description>
    </property>

    <!-- 静态用户配置（Web UI） -->
    <property>
        <name>hadoop.http.staticuser.user</name>
        <value>${HADOOP_USER}</value>
        <description>Web UI的匿名用户</description>
    </property>
</configuration>
EOF

    # 生成hdfs-site.xml模板
    cat > /tmp/hdfs-site.xml.template << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <!-- 副本数 -->
    <property>
        <name>dfs.replication</name>
        <value>3</value>
        <description>数据块副本数量</description>
    </property>

    <!-- NameNode RPC 地址 -->
    <property>
        <name>dfs.namenode.rpc-address</name>
        <value>${NAMENODE_NODE}:9820</value>
        <description>NameNode RPC服务地址</description>
    </property>

    <!-- NameNode HTTP 地址 -->
    <property>
        <name>dfs.namenode.http-address</name>
        <value>${NAMENODE_NODE}:9870</value>
        <description>NameNode Web UI地址</description>
    </property>

    <!-- SecondaryNameNode HTTP 地址 -->
    <property>
        <name>dfs.namenode.secondary.http-address</name>
        <value>${SECONDARY_NODE}:9868</value>
        <description>SecondaryNameNode Web UI地址</description>
    </property>

    <!-- NameNode 元数据存储目录 -->
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>file://${DATA_DIR}/hdfs/name</value>
        <description>NameNode元数据存储目录</description>
    </property>

    <!-- DataNode 数据存储目录 -->
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>file://${DATA_DIR}/hdfs/data</value>
        <description>DataNode数据存储目录</description>
    </property>

    <!-- 数据块大小 -->
    <property>
        <name>dfs.blocksize</name>
        <value>128m</value>
        <description>HDFS数据块大小</description>
    </property>

    <!-- 权限检查（开发环境可关闭） -->
    <property>
        <name>dfs.permissions.enabled</name>
        <value>false</value>
        <description>是否启用HDFS权限检查</description>
    </property>
</configuration>
EOF

    # 生成yarn-site.xml模板
    cat > /tmp/yarn-site.xml.template << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <!-- ResourceManager 主机名 -->
    <property>
        <name>yarn.resourcemanager.hostname</name>
        <value>${RESOURCEMANAGER_NODE}</value>
        <description>ResourceManager所在主机</description>
    </property>

    <!-- ResourceManager Web UI地址 -->
    <property>
        <name>yarn.resourcemanager.webapp.address</name>
        <value>${RESOURCEMANAGER_NODE}:8088</value>
        <description>ResourceManager Web UI地址</description>
    </property>

    <!-- NodeManager 辅助服务 -->
    <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
        <description>NodeManager辅助服务</description>
    </property>

    <!-- NodeManager 可用内存 -->
    <property>
        <name>yarn.nodemanager.resource.memory-mb</name>
        <value>8192</value>
        <description>NodeManager可用内存（MB）</description>
    </property>

    <!-- NodeManager 可用CPU核数 -->
    <property>
        <name>yarn.nodemanager.resource.cpu-vcores</name>
        <value>8</value>
        <description>NodeManager可用CPU核数</description>
    </property>

    <!-- 启用日志聚合 -->
    <property>
        <name>yarn.log-aggregation-enable</name>
        <value>true</value>
        <description>启用日志聚合功能</description>
    </property>

    <!-- JobHistory Server地址 -->
    <property>
        <name>yarn.log.server.url</name>
        <value>http://${JOBHISTORY_NODE}:19888/jobhistory/logs</value>
        <description>JobHistory Server日志URL</description>
    </property>

    <!-- 日志保留时间 -->
    <property>
        <name>yarn.log-aggregation.retain-seconds</name>
        <value>604800</value>
        <description>日志保留时间（秒）</description>
    </property>

	<!--环境变量的继承.3.1.3的bug.3.2.x时，就不需要再配置环境变量了-->
	<property>
		<name>yarn.nodemanager.env-whitelist</name>
	<value>JAVA_HOME,HADOOP_COMMON_HOME,HADOOP_HDFS_HOME,HADOOP_CONF_DIR,CLASSPATH_PREPEND_DISTCACHE,HADOOP_YARN_HOME,HADOOP_HOME,HADOOP_MAPRED_HOME</value>
	</property>
</configuration>
EOF

    # 生成mapred-site.xml模板
    cat > /tmp/mapred-site.xml.template << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <!-- MapReduce框架 -->
    <property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
        <description>指定MapReduce运行在YARN上</description>
    </property>

    <!-- JobHistory Server地址 -->
    <property>
        <name>mapreduce.jobhistory.address</name>
        <value>${JOBHISTORY_NODE}:10020</value>
        <description>JobHistory Server RPC地址</description>
    </property>

    <!-- JobHistory Server Web UI地址 -->
    <property>
        <name>mapreduce.jobhistory.webapp.address</name>
        <value>${JOBHISTORY_NODE}:19888</value>
        <description>JobHistory Server Web UI地址</description>
    </property>

    <!-- Map任务内存设置 -->
    <property>
        <name>mapreduce.map.memory.mb</name>
        <value>2048</value>
        <description>Map任务内存（MB）</description>
    </property>

    <!-- Reduce任务内存设置 -->
    <property>
        <name>mapreduce.reduce.memory.mb</name>
        <value>4096</value>
        <description>Reduce任务内存（MB）</description>
    </property>

    <!-- 启用Map输出压缩 -->
    <property>
        <name>mapreduce.map.output.compress</name>
        <value>true</value>
        <description>启用Map输出压缩</description>
    </property>

    <!-- Map输出压缩编码器 -->
    <property>
        <name>mapreduce.map.output.compress.codec</name>
        <value>org.apache.hadoop.io.compress.SnappyCodec</value>
        <description>Map输出压缩编码器</description>
    </property>
</configuration>
EOF

    # 生成workers文件模板
    cat > /tmp/workers.template << 'EOF'
# Hadoop集群工作节点列表
# 每行一个节点主机名或IP

${ALL_NODES}
EOF

    # 生成环境变量模板
    cat > /tmp/hadoop-profile.template << 'EOF'
# Hadoop环境变量配置
export HADOOP_HOME=${HADOOP_HOME}
export HADOOP_CONF_DIR=${HADOOP_HOME}/etc/hadoop
export HADOOP_MAPRED_HOME=${HADOOP_HOME}
export HADOOP_COMMON_HOME=${HADOOP_HOME}
export HADOOP_HDFS_HOME=${HADOOP_HOME}
export HADOOP_CLASSPATH=$(${HADOOP_HOME}/bin/hadoop classpath 2>/dev/null)
export YARN_HOME=${HADOOP_HOME}
export PATH=${PATH}:${HADOOP_HOME}/bin:${HADOOP_HOME}/sbin

# Java环境
export JAVA_HOME=${JAVA_HOME}
export PATH=${PATH}:${JAVA_HOME}/bin
EOF

    print_success "配置模板生成完成"
    print_step_complete
}

# 模块8：配置文件渲染
module_config_render() {
    print_step "配置渲染" "将模板渲染为实际配置文件"

    local conf_dir="$HADOOP_HOME/etc/hadoop"

    # 准备替换变量
    local vars="
    s|\\\${HADOOP_USER}|$HADOOP_USER|g
    s|\\\${HADOOP_GROUP}|$HADOOP_GROUP|g
    s|\\\${HADOOP_HOME}|$HADOOP_HOME|g
    s|\\\${DATA_DIR}|$DATA_DIR|g
    s|\\\${LOG_DIR}|$LOG_DIR|g
    s|\\\${PID_DIR}|$PID_DIR|g
    s|\\\${JAVA_HOME}|$JAVA_HOME|g
    s|\\\${NAMENODE_NODE}|$NAMENODE_NODE|g
    s|\\\${RESOURCEMANAGER_NODE}|$RESOURCEMANAGER_NODE|g
    s|\\\${SECONDARY_NODE}|$SECONDARY_NODE|g
    s|\\\${JOBHISTORY_NODE}|$JOBHISTORY_NODE|g
    s|\\\${ALL_NODES}|$ALL_NODES|g
    "

    # 渲染配置文件
    local templates=(
        "hadoop-env.sh"
        "core-site.xml"
        "hdfs-site.xml"
        "yarn-site.xml"
        "mapred-site.xml"
        "workers"
    )

    for template in "${templates[@]}"; do
        if [ -f "/tmp/$template.template" ]; then
			 # 特殊处理：hadoop-env.sh 中的 ${JAVA_HOME} 应该保留为变量，不进行替换
            if [ "$template" = "hadoop-env.sh" ]; then
                # 对于 hadoop-env.sh，我们只替换非 JAVA_HOME 的变量
                local env_vars="
                s|\\\${HADOOP_USER}|$HADOOP_USER|g
                s|\\\${HADOOP_HOME}|$HADOOP_HOME|g
                s|\\\${LOG_DIR}|$LOG_DIR|g
                s|\\\${PID_DIR}|$PID_DIR|g
                "
                sed "$env_vars" "/tmp/$template.template" > "$conf_dir/$template"
            else
                sed "$vars" "/tmp/$template.template" > "$conf_dir/$template"
            fi

            sudo chown "$HADOOP_USER:$HADOOP_GROUP" "$conf_dir/$template"
            print_info "生成配置文件: $template"
        fi
    done

    # 渲染环境变量文件。
    sed "$vars" "/tmp/hadoop-profile.template" > "/tmp/hadoop-profile.sh"
    sudo cp "/tmp/hadoop-profile.sh" "/etc/profile.d/hadoop.sh"

    print_success "配置文件渲染完成"
    print_step_complete
}

# 模块9：配置分发
module_config_distribute() {
    print_step "配置分发" "将配置分发到所有集群节点"

	local current_host=$(hostname)

	# 先测试SSH免密登录是否正常工作
    print_info "检查SSH免密登录状态..."

    local ssh_working_nodes=()
    local ssh_problem_nodes=()

    for node in $ALL_NODES; do
        if [ "$node" != "$current_host" ]; then
            # 测试SSH免密登录
            if sudo -u "$HADOOP_USER" ssh -o ConnectTimeout=5 -o BatchMode=yes "$node" "exit" 2>/dev/null; then
                ssh_working_nodes+=("$node")
                print_success "节点 $node: SSH免密登录正常"
            else
                ssh_problem_nodes+=("$node")
                print_warning "节点 $node: SSH免密登录有问题，将使用密码或备选方案"
            fi
        fi
    done

    # 如果有SSH问题的节点，尝试使用sshpass
    local use_sshpass=false
    if [ ${#ssh_problem_nodes[@]} -gt 0 ] && command -v sshpass &> /dev/null; then
        echo -e "${YELLOW}检测到sshpass工具，是否使用密码自动登录？(y/n): ${NC}\c"
        read -r use_sshpass_choice

        if [[ "$use_sshpass_choice" =~ ^[Yy]$ ]]; then
            use_sshpass=true
            print_info "将使用sshpass进行密码自动登录"
        fi
    fi

    # 分发函数，支持两种模式
    distribute_with_ssh() {
        local node="$1"
        local cmd="$2"

        if [ "$use_sshpass" = true ] && [ -n "$HADOOP_PASSWORD" ]; then
            # 使用sshpass执行命令
            sshpass -p "$HADOOP_PASSWORD" sudo -u "$HADOOP_USER" ssh -o StrictHostKeyChecking=no "$HADOOP_USER@$node" "$cmd"
        else
            # 使用普通SSH（期望免密登录）
            sudo -u "$HADOOP_USER" ssh -o StrictHostKeyChecking=no "$HADOOP_USER@$node" "$cmd"
        fi
    }

    distribute_file_with_scp() {
        local src="$1"
        local node="$2"
        local dst="$3"

        if [ "$use_sshpass" = true ] && [ -n "$HADOOP_PASSWORD" ]; then
            # 使用sshpass执行scp
            sshpass -p "$HADOOP_PASSWORD" scp -o StrictHostKeyChecking=no "$src" "$HADOOP_USER@$node:$dst"
        else
            # 使用普通scp（期望免密登录）
            sudo -u "$HADOOP_USER" scp -o StrictHostKeyChecking=no "$src" "$HADOOP_USER@$node:$dst"
        fi
    }

    # 定义要分发的文件和目录
    local distribute_items=(
        "$HADOOP_HOME"
        "/etc/profile.d/hadoop.sh"
        "/etc/sudoers.d/hadoop-$HADOOP_USER"
    )

	# 在主节点读取文件内容
	local profile_content
	if [ -f "/tmp/hadoop-profile.sh" ]; then
		profile_content=$(cat "/tmp/hadoop-profile.sh")
	else
		print_warning "主节点环境变量文件不存在，跳过"
		continue
	fi

    for node in $ALL_NODES; do

		# 获取节点的实际Java路径
		print_info "获取节点 $node 的实际Java路径..."
		local node_java_home=$(distribute_with_ssh "$node" "
			if command -v java &> /dev/null; then
				java_cmd=\$(which java)
				if [ -L \"\$java_cmd\" ]; then
					java_cmd=\$(readlink -f \"\$java_cmd\")
				fi
				dirname \"\$(dirname \"\$java_cmd\")\"
			else
				echo ''
			fi
		" 2>/dev/null)

		if [ -n "$node_java_home" ] && [ -d "$node_java_home" ]; then
			print_success "节点 $node 的Java路径: $node_java_home"
		else
			print_warning "节点 $node 无法获取Java路径，使用默认: /usr/lib/jvm/java-11-openjdk"
			node_java_home="/usr/lib/jvm/java-11-openjdk"
		fi

        if [ "$node" != "$(hostname)" ]; then
            print_info "分发配置到节点: $node"

            # 创建目标目录
            #sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "sudo mkdir -p $(dirname $HADOOP_HOME)"

            # 1. 在远程节点上创建目标目录并设置权限（使用sudo）
            print_info "在节点 $node 上创建目录"
            distribute_with_ssh "$node" "
			sudo mkdir -p $(dirname $HADOOP_HOME)
			sudo mkdir -p '$DATA_DIR/hdfs/name'
			sudo mkdir -p '$DATA_DIR/hdfs/data'
			sudo mkdir -p '$DATA_DIR/yarn/local'
			sudo mkdir -p '$DATA_DIR/yarn/logs'
			sudo mkdir -p '$DATA_DIR/tmp'
			sudo mkdir -p '$LOG_DIR'
			sudo mkdir -p '$PID_DIR'
			sudo mkdir -p '/tmp/hadoop'

			# 设置所有者和权限（需要在创建后设置）
			echo '设置目录所有者和权限...'
			sudo chown -R '$HADOOP_USER:$HADOOP_GROUP' '$(dirname $HADOOP_HOME)'
			sudo chown -R '$HADOOP_USER:$HADOOP_GROUP' '$DATA_DIR'
			sudo chown -R '$HADOOP_USER:$HADOOP_GROUP' '$LOG_DIR'
			sudo chown -R '$HADOOP_USER:$HADOOP_GROUP' '$PID_DIR'
			sudo chown -R '$HADOOP_USER:$HADOOP_GROUP' '/tmp/hadoop'

			# 设置目录权限
			sudo chmod -R 755 '$(dirname $HADOOP_HOME)'
			sudo chmod -R 755 '$DATA_DIR'
			sudo chmod -R 755 '$LOG_DIR'
			sudo chmod -R 755 '$PID_DIR'
			sudo chmod -R 777 '/tmp/hadoop'
			" 2>/dev/null || print_warning "节点 $node 目录创建失败"

            # 2. 分发Hadoop安装目录（排除不需要的文件,使用tar+ssh，避免权限问题）
            # 先检查远程目录是否可写
            if distribute_with_ssh "$node" "test -w $(dirname $HADOOP_HOME)" 2>/dev/null; then
                # 使用tar管道传输
                cd "$(dirname $HADOOP_HOME)"
                tar czf - "$(basename $HADOOP_HOME)" 2>/dev/null | \
                    distribute_with_ssh "$node" "tar xzf - -C $(dirname $HADOOP_HOME) && chmod -R 755 $HADOOP_HOME" 2>/dev/null

                if [ $? -ne 0 ]; then
                    print_warning "tar传输失败，尝试rsync..."
                    # 使用rsync
                    rsync -avz --exclude='logs/*' --exclude='*.pid' "$HADOOP_HOME/" "$HADOOP_USER@$node:$HADOOP_HOME/" 2>/dev/null || true
                fi
            else
                print_warning "目录不可写，尝试使用sudo..."
                # 使用sudo创建并设置权限
                distribute_with_ssh "$node" "sudo tar xzf - -C $(dirname $HADOOP_HOME) && sudo chown -R $HADOOP_USER:$HADOOP_GROUP $HADOOP_HOME" 2>/dev/null < <(tar czf - -C "$(dirname $HADOOP_HOME)" "$(basename $HADOOP_HOME)") 2>/dev/null
            fi

            # 3. 分发环境变量文件
            print_info "分发环境变量配置"
			# 在远程节点上修改JAVA_HOME
            distribute_with_ssh "$node" "
				# 创建临时文件
				cat > /tmp/hadoop-profile-template.sh << 'EOF'
${profile_content}
EOF
                # 备份原始文件
                if [ -f /tmp/hadoop-profile-template.sh ]; then

					# 获取原文件中的旧Java路径
					old_java_path=\$(grep 'export JAVA_HOME=' /tmp/hadoop-profile-template.sh | cut -d'=' -f2)

                    # 使用节点的实际Java路径替换JAVA_HOME
                    sed -i \"s|export JAVA_HOME=.*|export JAVA_HOME=${node_java_home}|\" /tmp/hadoop-profile-template.sh

					if [ -n \"\$old_java_path\" ]; then
						# 替换PATH中的旧Java路径
						sed -i \"s|:\${old_java_path}/bin|:${node_java_home}/bin|g\" /tmp/hadoop-profile-template.sh
					fi

                    # 安装修改后的文件
                    sudo cp /tmp/hadoop-profile-template.sh /etc/profile.d/hadoop.sh
                    sudo chmod 644 /etc/profile.d/hadoop.sh
					echo \"环境变量文件已更新\"
                else
                    echo '错误：找不到原始环境变量文件'
                fi
            " 2>/dev/null || print_warning "节点 $node 环境变量设置失败"

			# 4. 分发sudoers文件
            print_info "分发sudoers配置"
            if [ -f "/etc/sudoers.d/hadoop-$HADOOP_USER" ]; then
                distribute_file_with_scp "/etc/sudoers.d/hadoop-$HADOOP_USER" "$node" "/tmp/hadoop-sudoers" 2>/dev/null
                distribute_with_ssh "$node" "sudo cp /tmp/hadoop-sudoers /etc/sudoers.d/hadoop-$HADOOP_USER && sudo chmod 440 /etc/sudoers.d/hadoop-$HADOOP_USER && sudo rm -f /tmp/hadoop-sudoers" 2>/dev/null || true
            fi

            print_success "节点 $node 配置分发完成"
        else
			#主节点需要更新环境变量配置文件
			if [ -f '/etc/profile.d/hadoop.sh' ]; then

				# 获取原文件中的旧Java路径
				local old_java_path=$(grep 'export JAVA_HOME=' /etc/profile.d/hadoop.sh | cut -d'=' -f2)

				# 使用节点的实际Java路径替换JAVA_HOME
				sed -i "s|export JAVA_HOME=.*|export JAVA_HOME=${node_java_home}|" /etc/profile.d/hadoop.sh

				# 同时更新PATH中的Java路径
				if [ -n "$old_java_path" ]; then
					# 替换PATH中的旧Java路径
					sed -i "s|:${old_java_path}/bin|:${node_java_home}/bin|g" /etc/profile.d/hadoop.sh
				fi

				echo "环境变量文件已更新"
				echo "JAVA_HOME设置为: ${node_java_home}"
			else
				echo "错误：找不到原始环境变量文件"
			fi
		fi
    done

	# 验证分发结果
	print_info "验证分发结果"
	local verification_passed=0
	local verification_total=0

	for node in $ALL_NODES; do
		if [ "$node" != "$(hostname)" ]; then
			((verification_total++))

			# 检查Hadoop是否成功分发
			if distribute_with_ssh "$node" "[ -f '$HADOOP_HOME/bin/hadoop' ]" 2>/dev/null; then
				((verification_passed++))
				print_success "节点 $node: Hadoop安装验证成功"
			else
				print_warning "节点 $node: Hadoop安装可能不完整"
			fi
		fi
	done

	if [ $verification_passed -eq $verification_total ]; then
		print_success "所有节点配置分发验证通过 ($verification_passed/$verification_total)"
	else
		print_warning "部分节点配置分发需要检查 ($verification_passed/$verification_total)"
	fi

    print_step_complete
}

# 创建一个专门的修复/home/hadoop_user/.bashrc文件的函数
fix_bashrc_on_node() {
    local node=$1
    local hadoop_user=$2

    print_info "修复节点 $node 的.bashrc文件..."

    sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" '
    # 备份文件
    backup_file="$HOME/.bashrc.backup.$(date +%Y%m%d%H%M%S)"
    cp ~/.bashrc "$backup_file"

    # 创建全新的.bashrc
    cat > ~/.bashrc << "EOF"
# .bashrc

# Source global definitions
if [ -f /etc/bashrc ]; then
    . /etc/bashrc
fi

# Uncomment the following line if you don't like systemctl's auto-paging feature:
# export SYSTEMD_PAGER=

# User specific aliases and functions

# 加载Hadoop环境变量
if [ -f /etc/profile.d/hadoop.sh ]; then
    source /etc/profile.d/hadoop.sh
fi
EOF

    echo "修复完成，备份在: $backup_file"
' 2>/dev/null

    print_success "节点 $node .bashrc修复成功" || \
    print_warning "节点 $node .bashrc修复失败"
}

# 模块10：环境变量生效
module_environment_setup() {
    print_step "环境设置" "设置Hadoop环境变量"

    # 为当前用户设置环境变量
    if ! grep -q "HADOOP_HOME" "/home/$HADOOP_USER/.bashrc"; then
        cat >> "/home/$HADOOP_USER/.bashrc" << EOF
# 加载Hadoop环境变量
if [ -f /etc/profile.d/hadoop.sh ]; then
    source /etc/profile.d/hadoop.sh
fi
EOF
        print_success "为 $HADOOP_USER 用户设置环境变量"
    fi

	# 3. 配置所有从节点的.bashrc
    for node in $ALL_NODES; do
        if [ "$node" != "$(hostname)" ]; then
            print_info "配置节点 $node 的.bashrc..."
             fix_bashrc_on_node "$node" "$HADOOP_USER"
        fi
    done

	# 立即生效（使用更安全的方法）
    if sudo -u "$HADOOP_USER" bash -c "source ~/.bashrc 2>/dev/null"; then
        print_success "环境变量立即生效成功"
    else
        print_warning "环境变量立即生效失败，可能需要重新登录"
    fi

	# 在所有节点生效
    for node in $ALL_NODES; do
        if [ "$node" != "$(hostname)" ]; then
            print_info "在节点 $node 上生效环境变量..."
            if sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "source ~/.bashrc 2>/dev/null"; then
                print_success "节点 $node 环境变量生效成功"
            else
                print_warning "节点 $node 环境变量生效失败"
            fi
        fi
    done

	# 5. 最终验证
    print_info "最终验证所有节点环境变量..."
    local all_nodes_valid=true
    for node in $ALL_NODES; do
        if sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "
            if [ -n \"\\\$JAVA_HOME\" ] && [ -n \"\\\$HADOOP_HOME\" ]; then
                echo '✓ 节点 $node: 环境变量已设置'
                exit 0
            else
                echo '✗ 节点 $node: 环境变量未正确设置'
                exit 1
            fi
        " 2>/dev/null; then
            print_success "节点 $node 环境变量验证通过"
        else
            print_warning "节点 $node 环境变量验证失败"
            all_nodes_valid=false
        fi
    done

	if [ "$all_nodes_valid" = true ]; then
        print_success "所有节点环境变量配置验证通过 ✓"
    else
        print_warning "部分节点环境变量配置需要检查"
    fi

    print_success "环境变量设置完成"
    print_step_complete
}

# 模块11：HDFS初始化
module_hdfs_init() {
    print_step "HDFS初始化" "格式化HDFS NameNode"

	# 添加调试信息
    print_info "检查环境变量状态..."
    print_info "当前用户: $(whoami)"
    print_info "HADOOP_HOME: $HADOOP_HOME"
    print_info "JAVA_HOME: $JAVA_HOME"
    print_info "PATH: $PATH"

    # 检查hdfs命令是否存在
    #if command -v hdfs &> /dev/null; then
    #    print_success "hdfs命令在PATH中找到"
    #    print_info "hdfs路径: $(which hdfs)"
    #else
    #    print_warning "hdfs命令不在PATH中"
    #    print_info "使用绝对路径: $HADOOP_HOME/bin/hdfs"
    #fi
    #
    ## 检查Java
    #if command -v java &> /dev/null; then
    #    print_success "java命令在PATH中找到"
    #    print_info "java路径: $(which java)"
    #else
    #    print_error "java命令不在PATH中"
    #    return 1
    #fi

    # 检查是否已格式化
    local name_dir="$DATA_DIR/hdfs/name"
    if [ -d "$name_dir" ] && [ "$(ls -A $name_dir 2>/dev/null)" ]; then
        print_warning "NameNode数据目录非空，可能已格式化"

        echo -e "${YELLOW}是否重新格式化HDFS？(y/n): ${NC}\c"
        read -r format_choice

        if [[ ! "$format_choice" =~ ^[Yy]$ ]]; then
            print_info "跳过HDFS格式化"
            print_step_complete
            return 0
        fi

        # 备份现有数据
        local backup_dir="$name_dir-backup-$(date +%Y%m%d-%H%M%S)"
        sudo mv "$name_dir" "$backup_dir"
        sudo mkdir -p "$name_dir"
        sudo chown -R "$HADOOP_USER:$HADOOP_GROUP" "$name_dir"
        print_info "现有数据备份到: $backup_dir"
    fi

    # 执行格式化
    print_info "执行HDFS格式化"

	# ========== 关键修复：使用完整路径，避免 sudo PATH 问题 ==========
    local hdfs_cmd="$HADOOP_HOME/bin/hdfs"

	# 执行格式化
    if sudo -u "$HADOOP_USER" HDFS_CMD="$hdfs_cmd" bash -c '
        # 加载hadoop用户的环境
        if [ -f ~/.bashrc ]; then
            source ~/.bashrc
        fi
        if [ -f /etc/profile.d/hadoop.sh ]; then
            source /etc/profile.d/hadoop.sh
        fi

        echo "=== 格式化环境 ==="
        echo "JAVA_HOME: $JAVA_HOME"
        echo "HADOOP_HOME: $HADOOP_HOME"
        echo "使用的hdfs命令: $HDFS_CMD"
        echo "Java验证:"
        java -version 2>&1 | head -3
        echo "=== 开始格式化 ==="

        # 执行格式化
        "$HDFS_CMD" namenode -format -force -nonInteractive 2>&1
    ' 2>&1 | tee /tmp/hdfs-format.log; then

        # 检查是否真正成功
        if grep -q -E "(successfully formatted|Storage directory.*has been|Exiting with status 0)" /tmp/hdfs-format.log; then
            print_success "HDFS格式化成功"

            # 显示成功信息
            print_info "成功信息:"
            grep -E "(successfully|has been|Exiting)" /tmp/hdfs-format.log
        else
            print_warning "命令执行完成但未找到标准成功标志"
            print_info "最后输出:"
            tail -5 /tmp/hdfs-format.log
        fi
    else
        print_error "HDFS格式化失败"

        # 显示错误信息
        print_info "错误详情:"
        grep -i "error\|fail\|not found" /tmp/hdfs-format.log | head -10 || tail -10 /tmp/hdfs-format.log

        return 1
    fi

    print_step_complete
}

# 模块12：防火墙配置
module_firewall_setup() {
    print_step "防火墙配置" "配置必要的防火墙规则"

    # 检查防火墙服务
    local firewall_cmd=""

    if command -v ufw &> /dev/null; then
        firewall_cmd="ufw"
    elif command -v firewall-cmd &> /dev/null; then
        firewall_cmd="firewalld"
    elif command -v iptables &> /dev/null; then
        firewall_cmd="iptables"
    else
        print_warning "未检测到防火墙工具，跳过配置"
        print_step_complete
        return 0
    fi

    # 配置端口规则
    local ports=("9820" "9870" "9866" "9864" "9868" "8088" "8042" "19888" "10020")

    for port in "${ports[@]}"; do
        case $firewall_cmd in
            ufw)
                sudo ufw allow "$port/tcp" > /dev/null 2>&1
                ;;
            firewalld)
                sudo firewall-cmd --permanent --add-port="$port/tcp" > /dev/null 2>&1
                ;;
            iptables)
                sudo iptables -A INPUT -p tcp --dport "$port" -j ACCEPT > /dev/null 2>&1
                ;;
        esac
    done

    # 应用配置
    case $firewall_cmd in
        ufw)
            sudo ufw reload > /dev/null 2>&1
            ;;
        firewalld)
            sudo firewall-cmd --reload > /dev/null 2>&1
            ;;
        iptables)
            sudo service iptables save > /dev/null 2>&1 2>/dev/null || true
            ;;
    esac

    print_success "防火墙规则配置完成"
    print_step_complete
}

# 模块13：集群启动
module_cluster_start() {
    print_step "集群启动" "启动Hadoop集群服务"

    # 确保使用完整路径
    local hadoop_sbin="$HADOOP_HOME/sbin"
    local hadoop_bin="$HADOOP_HOME/bin"

    # 启动HDFS，NameNode 102
    print_info "启动HDFS"
     if [ -f "$hadoop_sbin/start-dfs.sh" ]; then
        #sudo -u "$HADOOP_USER" "$hadoop_sbin/start-dfs.sh"

		sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$NAMENODE_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi

# 启动HDFS
"$HADOOP_HOME/sbin/start-dfs.sh"
EOF
     2>/dev/null
    else
        print_error "找不到 start-dfs.sh: $hadoop_sbin/start-dfs.sh"
        return 1
    fi

    # 启动YARN 103
    print_info "启动YARN"
    if [ -f "$hadoop_sbin/start-yarn.sh" ]; then
        #sudo -u "$HADOOP_USER" "$hadoop_sbin/start-yarn.sh"

		sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$RESOURCEMANAGER_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi

# 启动YARN
"$HADOOP_HOME/sbin/start-yarn.sh"
EOF
     2>/dev/null
        print_success "YARN启动命令已执行"
    else
        print_error "找不到 start-yarn.sh: $hadoop_sbin/start-yarn.sh"
        return 1
    fi

    # 启动JobHistory Server
    print_info "启动JobHistory Server"
    if [ -f "$hadoop_bin/mapred" ]; then
        #sudo -u "$HADOOP_USER" "$hadoop_bin/mapred" --daemon start historyserver

		sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$JOBHISTORY_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi

# 启动JobHistory Server
"$HADOOP_HOME/bin/mapred" --daemon start historyserver
EOF
     2>/dev/null
        print_success "JobHistory Server启动命令已执行"
    else
        print_error "找不到 mapred 命令: $hadoop_bin/mapred"
    fi

    # 单独启动SecondaryNameNode 104
    print_info "启动SecondaryNameNode"
    local hdfs_cmd="$HADOOP_HOME/bin/hdfs"

    if [ -f "$hdfs_cmd" ]; then
        #sudo -u "$HADOOP_USER" ssh "$SECONDARY_NODE" "'$hdfs_cmd' --daemon start secondarynamenode"

		sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$SECONDARY_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi

# 单独启动SecondaryNameNode
"$HADOOP_HOME/bin/hdfs" --daemon start
EOF
     2>/dev/null
        print_success "SecondaryNameNode启动命令已执行"
    else
        print_error "找不到 hdfs 命令: $hdfs_cmd"
    fi

    # 等待服务启动
    print_info "等待服务启动..."
    sleep 10

    print_step_complete
}

# 模块14：集群验证
module_cluster_validate() {
    print_step "集群验证" "验证Hadoop集群功能"

    local validation_passed=0
    local validation_total=5

	local hadoop_bin="$HADOOP_HOME/bin"

    # 测试1: HDFS基本操作
    print_info "测试1: HDFS基本操作..."
    sudo -u "$HADOOP_USER" HADOOP_BIN="$hadoop_bin"  bash -c '
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi
"$HADOOP_BIN/hdfs" dfs -mkdir -p /test-validation

"$HADOOP_BIN/hdfs" dfs -put /etc/hosts /test-validation/hosts-copy
'
    #sudo -u "$HADOOP_USER" hdfs dfs -put /etc/hosts /test-validation/hosts-copy
    local hdfs_test_result=$?

    if [ $hdfs_test_result -eq 0 ]; then
        print_success "HDFS基本操作测试通过"
        validation_passed=$((validation_passed + 1))
    else
        print_error "HDFS基本操作测试失败"
    fi

    # 测试2: HDFS状态检查
    print_info "测试2: HDFS状态检查..."
    #sudo -u "$HADOOP_USER" hdfs dfsadmin -report 2>&1 | grep -q "Live datanodes"
	sudo -u "$HADOOP_USER" HADOOP_BIN="$hadoop_bin"  bash -c '
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi
"$HADOOP_BIN/hdfs" dfsadmin -report
'
    local hdfs_status_result=$?

    if [ $hdfs_status_result -eq 0 ]; then
        local datanodes_count=$(sudo -u "$HADOOP_USER" HADOOP_BIN="$hadoop_bin" bash -c '
		# 加载hadoop用户的环境
		if [ -f ~/.bashrc ]; then
			source ~/.bashrc
		fi
		if [ -f /etc/profile.d/hadoop.sh ]; then
			source /etc/profile.d/hadoop.sh
		fi
		"$HADOOP_BIN/hdfs" dfsadmin -report
		' 2>&1 | grep "Live datanodes" | awk '{print $3}')
        print_success "HDFS状态正常，活跃DataNode数: $datanodes_count"
        validation_passed=$((validation_passed + 1))
    else
        print_error "HDFS状态检查失败"
    fi

    # 测试3: YARN状态检查
    print_info "测试3: YARN状态检查..."
    sudo -u "$HADOOP_USER" HADOOP_BIN="$hadoop_bin" bash -c '
	# 加载hadoop用户的环境
	if [ -f ~/.bashrc ]; then
		source ~/.bashrc
	fi
	if [ -f /etc/profile.d/hadoop.sh ]; then
		source /etc/profile.d/hadoop.sh
	fi
	"$HADOOP_BIN/yarn" node -list
	' 2>&1 | grep -q "Total Nodes"
    local yarn_status_result=$?

    if [ $yarn_status_result -eq 0 ]; then
        local yarn_nodes_count=$(sudo -u "$HADOOP_USER" HADOOP_BIN="$hadoop_bin" bash -c '
		# 加载hadoop用户的环境
		if [ -f ~/.bashrc ]; then
			source ~/.bashrc
		fi
		if [ -f /etc/profile.d/hadoop.sh ]; then
			source /etc/profile.d/hadoop.sh
		fi
		"$HADOOP_BIN/yarn" node -list
		' 2>&1 | grep "Total Nodes" | awk '{print $3}')
        print_success "YARN状态正常，节点数: $yarn_nodes_count"
        validation_passed=$((validation_passed + 1))
    else
        print_error "YARN状态检查失败"
    fi

    # 测试4: MapReduce示例作业
    print_info "测试4: MapReduce示例作业..."
    local test_output="/test-validation/output-$(date +%s)"
    sudo -u "$HADOOP_USER" HADOOP_HOME="$HADOOP_HOME" HADOOP_BIN="$hadoop_bin" HADOOP_VERSION="$HADOOP_VERSION" TEST_OUTPUT="$test_output" bash -c '
	# 加载hadoop用户的环境
	if [ -f ~/.bashrc ]; then
		source ~/.bashrc
	fi
	if [ -f /etc/profile.d/hadoop.sh ]; then
		source /etc/profile.d/hadoop.sh
	fi
	echo -e "Hadoop\nCluster\nValidation\nTest" | "$HADOOP_BIN/hdfs" dfs -put - /test-validation/test-input.txt

	sleep 3

	echo "HADOOP_HOME: $HADOOP_HOME"
    echo "HADOOP_CLASSPATH: $HADOOP_CLASSPATH"
    echo "HADOOP_MAPRED_HOME: $HADOOP_MAPRED_HOME"


	"$HADOOP_BIN/hadoop" jar "$HADOOP_HOME/share/hadoop/mapreduce/hadoop-mapreduce-examples-$HADOOP_VERSION.jar" wordcount /test-validation/test-input.txt "$TEST_OUTPUT"
	' 2>&1 | tail -50

    local mapreduce_test_result=$?
    if [ $mapreduce_test_result -eq 0 ]; then
        print_success "MapReduce示例作业测试通过"
        validation_passed=$((validation_passed + 1))
    else
        print_warning "MapReduce示例作业测试可能有警告"
    fi

    # 测试5: Web UI访问检查
    print_info "测试5: Web UI服务检查..."
	if check_http_service "http://$NAMENODE_NODE:9870" "NameNode" 15; then
         print_success "Web UI访问检查测试通过"
        validation_passed=$((validation_passed + 1))
    fi

    # 清理测试数据
    sudo -u "$HADOOP_USER" HADOOP_BIN="$hadoop_bin" bash -c '
	# 加载hadoop用户的环境
	if [ -f ~/.bashrc ]; then
		source ~/.bashrc
	fi
	if [ -f /etc/profile.d/hadoop.sh ]; then
		source /etc/profile.d/hadoop.sh
	fi
	"$HADOOP_BIN/hdfs" dfs -rm -r -f /test-validation'

    if [ $validation_passed -eq $validation_total ]; then
        print_success "集群验证完成，所有测试通过 ($validation_passed/$validation_total)"
    else
        print_warning "集群验证完成，部分测试通过 ($validation_passed/$validation_total)"
    fi

    print_step_complete
}

# 验证HTTP服务是否可访问
check_http_service() {
    local url="$1"
    local service_name="$2"
    local timeout="${3:-10}"  # 默认10秒超时

    print_info "检查 $service_name 服务 ($url)..."

    # 使用curl检查
    if curl -s -f --max-time "$timeout" --head "$url" > /dev/null 2>&1; then
        print_success "$service_name 可访问"
        return 0
    else
        print_warning "$service_name 不可访问"
        return 1
    fi
}

# 模块15：访问信息显示
module_access_info() {
    print_step "访问信息" "显示集群访问信息"

    # 获取节点IP
    get_ip() {
        local node=$1
        sudo -u $HADOOP_USER ssh "$HADOOP_USER@$node" "hostname -I | awk '{print \$1}' 2>/dev/null || echo '$node'"
    }

    local namenode_ip=$(get_ip "$NAMENODE_NODE")
    local resourcemanager_ip=$(get_ip "$RESOURCEMANAGER_NODE")
    local secondary_ip=$(get_ip "$SECONDARY_NODE")
    local jobhistory_ip=$(get_ip "$JOBHISTORY_NODE")

    # 显示访问信息
    echo -e "\n${GREEN}${BOLD}══════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}${BOLD}  Hadoop集群部署完成！${NC}"
    echo -e "${GREEN}${BOLD}══════════════════════════════════════════════════════════${NC}"

    echo -e "\n${CYAN}${BOLD}📡 Web UI访问地址:${NC}"
    echo -e "  ${WHITE}NameNode:${NC}           ${GREEN}http://${namenode_ip}:9870${NC}"
    echo -e "  ${WHITE}ResourceManager:${NC}    ${GREEN}http://${resourcemanager_ip}:8088${NC}"
    echo -e "  ${WHITE}SecondaryNameNode:${NC}  ${GREEN}http://${secondary_ip}:9868${NC}"
    echo -e "  ${WHITE}JobHistory Server:${NC}  ${GREEN}http://${jobhistory_ip}:19888${NC}"

    echo -e "\n${CYAN}${BOLD}🔧 服务端点:${NC}"
    echo -e "  ${WHITE}HDFS RPC:${NC}           ${GREEN}hdfs://${NAMENODE_NODE}:9820${NC}"
    echo -e "  ${WHITE}DataNode Web UI:${NC}"
    for node in $ALL_NODES; do
        local node_ip=$(get_ip "$node")
        echo -e "    - $node: ${GREEN}http://${node_ip}:9864${NC}"
    done

    echo -e "\n${CYAN}${BOLD}💻 命令行工具:${NC}"
    echo -e "  ${WHITE}HDFS操作:${NC}           ${GREEN}hdfs dfs -ls /${NC}"
    echo -e "  ${WHITE}YARN应用列表:${NC}       ${GREEN}yarn application -list${NC}"
    echo -e "  ${WHITE}集群状态:${NC}           ${GREEN}hdfs dfsadmin -report${NC}"

    echo -e "\n${CYAN}${BOLD}📊 集群信息:${NC}"
    echo -e "  ${WHITE}集群名称:${NC}           ${YELLOW}$CLUSTER_NAME${NC}"
    echo -e "  ${WHITE}Hadoop版本:${NC}         ${YELLOW}$HADOOP_VERSION${NC}"
    echo -e "  ${WHITE}运行用户:${NC}           ${YELLOW}$HADOOP_USER${NC}"
    echo -e "  ${WHITE}数据目录:${NC}           ${YELLOW}$DATA_DIR${NC}"

    echo -e "\n${YELLOW}${BOLD}📋 下一步建议:${NC}"
    echo -e "  1. 将访问地址添加到书签"
    echo -e "  2. 检查防火墙确保端口可访问"
    echo -e "  3. 运行测试作业验证集群功能"
    echo -e "  4. 配置监控和告警"

    echo -e "\n${GREEN}${BOLD}══════════════════════════════════════════════════════════${NC}"

    # 保存配置到文件
    cat > "$CONFIG_FILE" << EOF
# Hadoop集群配置备份 - $(date)
CLUSTER_NAME="$CLUSTER_NAME"
HADOOP_VERSION="$HADOOP_VERSION"
HADOOP_USER="$HADOOP_USER"
HADOOP_HOME="$HADOOP_HOME"
JAVA_HOME="$JAVA_HOME"

# 节点配置
MASTER_NODE="$MASTER_NODE"
WORKER_NODES="$WORKER_NODES"
ALL_NODES="$ALL_NODES"

# 服务分配
NAMENODE_NODE="$NAMENODE_NODE"
RESOURCEMANAGER_NODE="$RESOURCEMANAGER_NODE"
SECONDARY_NODE="$SECONDARY_NODE"
JOBHISTORY_NODE="$JOBHISTORY_NODE"

# 访问信息
NAMENODE_WEB="http://$namenode_ip:9870"
RESOURCEMANAGER_WEB="http://$resourcemanager_ip:8088"
HDFS_RPC="hdfs://$NAMENODE_NODE:9820"

# 日志文件
SETUP_LOG="$LOG_FILE"
EOF

    print_info "配置已保存到: $CONFIG_FILE"
    print_step_complete
}

# 模块：启动集群服务
module_start_cluster() {
    print_step "启动集群服务" "启动Hadoop集群的所有服务"

	# 获取当前主机名
    local current_host=$(hostname)

    print_info "当前主机: $current_host"
    print_info "NameNode节点: $NAMENODE_NODE"
    print_info "SecondaryNameNode节点: $SECONDARY_NODE"
    print_info "ResourceManager节点: $RESOURCEMANAGER_NODE"
    print_info "JobHistory节点: $JOBHISTORY_NODE"

    # 确保使用完整路径
    local hadoop_sbin="$HADOOP_HOME/sbin"
    local hadoop_bin="$HADOOP_HOME/bin"

    # 清理旧的PID文件（避免启动失败）
    print_info "清理旧的PID文件..."
    for node in $ALL_NODES; do
		print_info "清理节点: $node"
        if [ "$node" = "$(hostname)" ]; then
            # 本地节点
            sudo rm -f /tmp/hadoop-*.pid /tmp/hadoop-hadoop-*.pid 2>/dev/null || true
        else
            # 远程节点
            sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "sudo rm -f /tmp/hadoop-*.pid /tmp/hadoop-hadoop-*.pid 2>/dev/null" || true
        fi
    done

    # 启动HDFS 102
    print_info "启动HDFS服务..."
    if [ -f "$hadoop_sbin/start-dfs.sh" ]; then
        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$NAMENODE_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi
"$HADOOP_HOME"/sbin/start-dfs.sh
EOF
        print_success "HDFS启动命令已执行"
    else
        print_error "找不到 start-dfs.sh"
        return 1
    fi

    # 启动YARN 103
    print_info "启动YARN服务..."
    if [ -f "$hadoop_sbin/start-yarn.sh" ]; then
        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$RESOURCEMANAGER_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi
$HADOOP_HOME/sbin/start-yarn.sh
EOF
        print_success "YARN启动命令已执行"
    else
        print_error "找不到 start-yarn.sh"
        return 1
    fi

    # 启动JobHistory Server
    print_info "启动JobHistory Server..."
    if [ -f "$hadoop_bin/mapred" ]; then
        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$RESOURCEMANAGER_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi
$HADOOP_HOME/bin/mapred --daemon start historyserver
EOF
        print_success "JobHistory Server启动命令已执行"
    else
        print_error "找不到 mapred 命令"
    fi

    # 等待服务启动
    print_info "等待服务启动..."
    sleep 10

    print_step_complete
}

# 手动启动失败的服务
manual_start_failed_services() {
    echo -e "\n${YELLOW}${BOLD}🔄 尝试手动启动失败的服务...${NC}"

	# 确保使用完整路径
    local hadoop_sbin="$HADOOP_HOME/sbin"
    local hadoop_bin="$HADOOP_HOME/bin"

    # 启动NameNode（如果失败）
	local nn_pid=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$NAMENODE_NODE" "jps | grep -i namenode | awk '{print \$1}'" 2>/dev/null || echo "")

    if [ -z "$nn_pid" ]; then
        print_info "手动启动NameNode (节点: $NAMENODE_NODE)..."
        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$NAMENODE_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi

echo "停止可能残留的NameNode进程..."
pkill -f "NameNode" 2>/dev/null || true
sleep 2

echo "清理PID文件..."
rm -f /tmp/hadoop-*-namenode.pid /tmp/hadoop-hadoop-namenode.pid 2>/dev/null || true

echo "启动NameNode..."
"$HADOOP_HOMe/bin/hdfs" --daemon start namenode

echo "等待5秒..."
sleep 5

echo "检查启动结果:"
jps | grep -i namenode || echo "NameNode启动失败"
EOF
         2>/dev/null || print_warning "NameNode启动失败"
    fi

    # 启动ResourceManager（如果失败）
	local rm_pid=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$RESOURCEMANAGER_NODE" "jps | grep -i resourcemanager | awk '{print \$1}'" 2>/dev/null || echo "")

    if [ -z "$rm_pid" ]; then
        print_info "手动启动ResourceManager (节点: $RESOURCEMANAGER_NODE)..."
        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$RESOURCEMANAGER_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi

echo "停止可能残留的ResourceManager进程..."
pkill -f "ResourceManager" 2>/dev/null || true
sleep 2

echo "清理PID文件..."
rm -f /tmp/hadoop-*-resourcemanager.pid /tmp/hadoop-hadoop-resourcemanager.pid 2>/dev/null || true

echo "启动ResourceManager..."
"$HADOOP_HOME/bin/yarn" --daemon start resourcemanager

echo "等待5秒..."
sleep 5

echo "检查启动结果:"
jps | grep -i resourcemanager || echo "ResourceManager启动失败"
EOF
         2>/dev/null || print_warning "ResourceManager启动失败"
    fi

    # 启动SecondaryNameNode
	local snn_pid=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$SECONDARY_NODE" "jps | grep -i secondarynamenode | awk '{print \$1}'" 2>/dev/null || echo "")

    if [ -z "$snn_pid" ]; then
        print_info "手动启动SecondaryNameNode (节点: $SECONDARY_NODE)..."
        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$SECONDARY_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi

echo "停止可能残留的SecondaryNameNode进程..."
pkill -f "SecondaryNameNode" 2>/dev/null || true
sleep 2

echo "清理PID文件..."
rm -f /tmp/hadoop-*-secondarynamenode.pid /tmp/hadoop-hadoop-secondarynamenode.pid 2>/dev/null || true

echo "启动SecondaryNameNode..."
"$HADOOP_HOME/bin/hdfs" --daemon start secondarynamenode

echo "等待5秒..."
sleep 5

echo "检查启动结果:"
jps | grep -i secondarynamenode || echo "SecondaryNameNode启动失败"
EOF
         2>/dev/null || print_warning "SecondaryNameNode启动失败"
    fi

	# 启动JobHistory Server（如果失败）
    local jhs_pid=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$JOBHISTORY_NODE" "jps | grep -i jobhistoryserver | awk '{print \$1}'" 2>/dev/null || echo "")
    if [ -z "$jhs_pid" ]; then
        print_info "手动启动JobHistory Server (节点: $JOBHISTORY_NODE)..."
        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$JOBHISTORY_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi

echo "停止可能残留的JobHistoryServer进程..."
pkill -f "JobHistoryServer" 2>/dev/null || true
sleep 2

echo "清理PID文件..."
rm -f /tmp/hadoop-*-jobhistoryserver.pid /tmp/hadoop-hadoop-jobhistoryserver.pid 2>/dev/null || true

echo "启动JobHistoryServer..."
"$HADOOP_HOME/bin/mapred" --daemon start historyserver

echo "等待5秒..."
sleep 5

echo "检查启动结果:"
jps | grep -i jobhistoryserver || echo "JobHistoryServer启动失败"
EOF
         2>/dev/null || print_warning "JobHistoryServer启动失败"
    fi

    # 启动缺失的DataNode如果失败）
    for node in $ALL_NODES; do
        local dn_pid=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "jps | grep -i datanode | awk '{print \$1}'" 2>/dev/null || echo "")
        if [ -z "$dn_pid" ]; then
            print_info "手动启动DataNode (节点: $node)..."
            sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi

echo "停止可能残留的DataNode进程..."
pkill -f "DataNode" 2>/dev/null || true
sleep 2

echo "清理PID文件..."
rm -f /tmp/hadoop-*-datanode.pid /tmp/hadoop-hadoop-datanode.pid 2>/dev/null || true

echo "检查数据目录..."
if [ ! -d "$DATA_DIR/hdfs/data" ]; then
	echo "创建数据目录..."
	sudo mkdir -p "$DATA_DIR/hdfs/data"
	sudo chown -R $HADOOP_USER:$HADOOP_GROUP "$DATA_DIR/hdfs/data"
	sudo chmod -R 755 "$DATA_DIR/hdfs/data"
fi

echo "启动DataNode..."
"$HADOOP_HOME/bin/hdfs" --daemon start datanode

echo "等待5秒..."
sleep 5

echo "检查启动结果:"
jps | grep -i datanode || echo "DataNode启动失败"
EOF
             2>/dev/null || print_warning "$node节点DataNode启动失败"
        fi
    done

    # 等待服务启动
    sleep 10
}

# 诊断Hadoop配置问题
# 诊断Hadoop配置问题（返回诊断结果）
diagnose_hadoop_issues() {
    local result=""

    # 检查JAVA_HOME设置
    result+="${CYAN}${BOLD}1. 检查各节点JAVA_HOME设置：${NC}\n"
    for node in $ALL_NODES; do
        result+="\n${WHITE}节点 $node:${NC}\n"
        local java_home_check=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" bash -c '
			# 加载hadoop用户的环境
			if [ -f ~/.bashrc ]; then
				source ~/.bashrc
			fi
			if [ -f /etc/profile.d/hadoop.sh ]; then
				source /etc/profile.d/hadoop.sh
			fi
            echo "JAVA_HOME: "\$JAVA_HOME
            command -v java 2>/dev/null || echo "Java命令未找到"
        ' 2>/dev/null)
        result+="$java_home_check\n"

        # 检查是否为空或未设置
        if echo "$java_home_check" | grep -q "JAVA_HOME=$" || echo "$java_home_check" | grep -q "Java命令未找到"; then
            result+="  ${RED}✗ Java环境有问题${NC}\n"
        else
            result+="  ${GREEN}✓ Java环境正常${NC}\n"
        fi
    done

    # 检查数据目录
    result+="\n${CYAN}${BOLD}2. 检查各节点数据目录：${NC}\n"
    for node in $ALL_NODES; do
        result+="\n${WHITE}节点 $node:${NC}\n"
        local data_dir_check=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "
            if [ -d '$DATA_DIR/hdfs' ]; then
                echo '数据目录存在'
                ls -la '$DATA_DIR/hdfs/' | head -5
            else
                echo '数据目录不存在'
            fi
        " 2>/dev/null)
        result+="$data_dir_check\n"

        # 检查数据目录是否存在
        if echo "$data_dir_check" | grep -q "数据目录不存在"; then
            result+="  ${RED}✗ 数据目录不存在${NC}\n"
        else
            result+="  ${GREEN}✓ 数据目录正常${NC}\n"
        fi
    done

    # 检查配置文件
    result+="\n${CYAN}${BOLD}3. 检查Hadoop配置文件：${NC}\n"
    local conf_dir="$HADOOP_HOME/etc/hadoop"

    # core-site.xml
    local fs_default=$(grep -A1 "fs.defaultFS" "$conf_dir/core-site.xml" 2>/dev/null || echo "配置文件不存在")
    result+="\n${WHITE}core-site.xml (fs.defaultFS):${NC}\n$fs_default\n"

    # yarn-site.xml
    local rm_hostname=$(grep -A1 "yarn.resourcemanager.hostname" "$conf_dir/yarn-site.xml" 2>/dev/null || echo "配置文件不存在")
    result+="\n${WHITE}yarn-site.xml (yarn.resourcemanager.hostname):${NC}\n$rm_hostname\n"

    # hdfs-site.xml
    local secondary_addr=$(grep -A1 "dfs.namenode.secondary.http-address" "$conf_dir/hdfs-site.xml" 2>/dev/null || echo "配置文件不存在")
    result+="\n${WHITE}hdfs-site.xml (dfs.namenode.secondary.http-address):${NC}\n$secondary_addr\n"

    echo -e "$result"
}

# 或者使用更简洁的诊断函数，返回退出码
diagnose_and_validate() {
    echo -e "\n${CYAN}${BOLD}🔧 Hadoop配置诊断：${NC}"

    local critical_errors=0

    # 检查Java环境
    print_info "检查各节点Java环境..."
    for node in $ALL_NODES; do
        local java_check=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "
            if command -v java &> /dev/null; then
                if [ -n \"\$JAVA_HOME\" ]; then
                    echo \"✓ Java环境正常 (JAVA_HOME: \$JAVA_HOME)\"
                    exit 0
                else
                    echo \"✗ JAVA_HOME未设置\"
                    exit 1
                fi
            else
                echo \"✗ Java命令未找到\"
                exit 2
            fi
        " 2>/dev/null)

        if [ $? -ne 0 ]; then
            echo -e "  ${RED}✗ $node: $java_check${NC}"
            critical_errors=$((critical_errors + 1))
        else
            echo -e "  ${GREEN}✓ $node: $java_check${NC}"
        fi
    done

    # 检查数据目录
    print_info "检查各节点数据目录..."
    for node in $ALL_NODES; do
        local data_dir_check=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "
            if [ -d '$DATA_DIR/hdfs/data' ] && [ -d '$DATA_DIR/hdfs/name' ]; then
                echo \"✓ 数据目录存在\"
                exit 0
            else
                echo \"✗ 数据目录不存在\"
                exit 1
            fi
        " 2>/dev/null)

        if [ $? -ne 0 ]; then
            echo -e "  ${RED}✗ $node: $data_dir_check${NC}"
            critical_errors=$((critical_errors + 1))
        else
            echo -e "  ${GREEN}✓ $node: $data_dir_check${NC}"
        fi
    done

    # 检查Hadoop配置文件
    print_info "检查Hadoop配置文件..."
    local conf_dir="$HADOOP_HOME/etc/hadoop"

    if [ ! -f "$conf_dir/core-site.xml" ]; then
        echo -e "  ${RED}✗ core-site.xml不存在${NC}"
        critical_errors=$((critical_errors + 1))
    else
        echo -e "  ${GREEN}✓ core-site.xml存在${NC}"
    fi

    if [ ! -f "$conf_dir/yarn-site.xml" ]; then
        echo -e "  ${RED}✗ yarn-site.xml不存在${NC}"
        critical_errors=$((critical_errors + 1))
    else
        echo -e "  ${GREEN}✓ yarn-site.xml存在${NC}"
    fi

    if [ ! -f "$conf_dir/hdfs-site.xml" ]; then
        echo -e "  ${RED}✗ hdfs-site.xml不存在${NC}"
        critical_errors=$((critical_errors + 1))
    else
        echo -e "  ${GREEN}✓ hdfs-site.xml存在${NC}"
    fi

    # 显示诊断结果
    echo -e "\n${CYAN}${BOLD}📊 诊断结果：${NC}"
    if [ $critical_errors -eq 0 ]; then
        echo -e "${GREEN}${BOLD}✅ 所有检查通过，可以启动集群${NC}"
        return 0
    else
        echo -e "${RED}${BOLD}❌ 发现 $critical_errors 个严重问题${NC}"
        echo -e "${YELLOW}请修复上述问题后重新启动集群。${NC}"
        return 1
    fi
}

# 修复集群问题函数
fix_cluster_issues() {
    echo -e "\n${BLUE}开始修复集群问题...${NC}"

    # 修复Java环境
    print_info "修复Java环境..."
    for node in $ALL_NODES; do
        print_info "修复节点 $node 的Java环境..."

        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "
            # 检测Java安装路径
            if command -v java &> /dev/null; then
                java_path=\$(which java)
                if [ -L \"\$java_path\" ]; then
                    java_path=\$(readlink -f \"\$java_path\")
                fi
                detected_java_home=\$(dirname \"\$(dirname \"\$java_path\")\")

                # 设置JAVA_HOME
                if [ -d \"\$detected_java_home\" ]; then
                    echo \"检测到Java安装路径: \$detected_java_home\"

                    # 添加到.bashrc
                    if ! grep -q \"JAVA_HOME=\" ~/.bashrc; then
                        echo \"export JAVA_HOME=\\\"\$detected_java_home\\\"\" >> ~/.bashrc
                        echo \"export PATH=\\\"\$JAVA_HOME/bin:\\\$PATH\\\"\" >> ~/.bashrc
                        echo \"已添加到.bashrc\"
                    fi

                    # 立即生效
                    export JAVA_HOME=\"\$detected_java_home\"
                    export PATH=\"\$JAVA_HOME/bin:\$PATH\"

                    echo \"JAVA_HOME设置为: \$JAVA_HOME\"
                else
                    echo \"无法确定JAVA_HOME\"
                fi
            else
                echo \"Java未安装\"
            fi
        " 2>/dev/null || print_warning "节点 $node Java环境修复失败"
    done

    # 修复数据目录
    print_info "修复数据目录..."
    for node in $ALL_NODES; do
        print_info "修复节点 $node 的数据目录..."

        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "
            # 创建数据目录
            sudo mkdir -p '$DATA_DIR/hdfs/data'
            sudo mkdir -p '$DATA_DIR/hdfs/name'
            sudo mkdir -p '$DATA_DIR/tmp'
            sudo mkdir -p '$DATA_DIR/yarn/local'
            sudo mkdir -p '$DATA_DIR/yarn/logs'

            # 设置权限
            sudo chown -R '$HADOOP_USER:$HADOOP_GROUP' '$DATA_DIR'
            sudo chmod -R 755 '$DATA_DIR'

            echo \"数据目录创建完成: $DATA_DIR\"
        " 2>/dev/null || print_warning "节点 $node 数据目录修复失败"
    done

    print_success "修复完成，请重新启动集群"
}


# 检查服务启动失败的具体原因
check_service_logs() {
    local service="$1"
    local node="$2"

    echo -e "\n${YELLOW}${BOLD}🔍 检查 $service 日志 ($node):${NC}"

    case $service in
        "NameNode")
            local log_pattern="namenode"
            ;;
        "ResourceManager")
            local log_pattern="resourcemanager"
            ;;
        "DataNode")
            local log_pattern="datanode"
            ;;
        "SecondaryNameNode")
            local log_pattern="secondarynamenode"
            ;;
        "JobHistoryServer")
            local log_pattern="jobhistory"
            ;;
        *)
            local log_pattern="$service"
            ;;
    esac

    # 查找并显示最新的日志
    sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "
        echo '查找 $service 日志文件...'
        find '$LOG_DIR' -name '*$log_pattern*.log' -type f 2>/dev/null | head -3

        echo ''
        echo '最新日志的最后20行:'
        find '$LOG_DIR' -name '*$log_pattern*.log' -type f 2>/dev/null | head -1 | xargs tail -20 2>/dev/null || echo '未找到日志文件'

        echo ''
        echo '检查是否有错误:'
        find '$LOG_DIR' -name '*$log_pattern*.log' -type f 2>/dev/null | head -1 | xargs grep -i 'error\|exception\|fatal\|failed' 2>/dev/null | head -5 || echo '未找到错误信息'
    " 2>/dev/null || echo "无法连接到节点 $node"
}


# 检查服务状态函数
check_service_status() {
    echo -e "\n${CYAN}${BOLD}🔍 服务状态检查：${NC}"

	local failed_services=()

    # 检查NameNode
    print_info "检查NameNode..."
    local nn_pid=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$NAMENODE_NODE" "jps | grep -i namenode | awk '{print \$1}'" 2>/dev/null || echo "")
    if [ -n "$nn_pid" ]; then
        echo -e "  ${GREEN}✓ NameNode${NC}       - 运行中 (PID: $nn_pid, 节点: $NAMENODE_NODE)"
    else
        echo -e "  ${RED}✗ NameNode${NC}       - 未运行 (应在节点: $NAMENODE_NODE)"
        failed_services+=("NameNode@$NAMENODE_NODE")
    fi

    # 检查ResourceManager
	print_info "检查ResourceManager..."
    local rm_pid=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$RESOURCEMANAGER_NODE" "jps | grep -i resourcemanager | awk '{print \$1}'" 2>/dev/null || echo "")
    if [ -n "$rm_pid" ]; then
        echo -e "  ${GREEN}✓ ResourceManager${NC} - 运行中 (PID: $rm_pid, 节点: $RESOURCEMANAGER_NODE)"
    else
        echo -e "  ${RED}✗ ResourceManager${NC} - 未运行 (应在节点: $RESOURCEMANAGER_NODE)"
        failed_services+=("ResourceManager@$RESOURCEMANAGER_NODE")
    fi

    # 检查JobHistory Server
	print_info "检查JobHistory Server..."
    local jhs_pid=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$JOBHISTORY_NODE" "jps | grep -i jobhistoryserver | awk '{print \$1}'" 2>/dev/null || echo "")
    if [ -n "$jhs_pid" ]; then
        echo -e "  ${GREEN}✓ JobHistory Server${NC} - 运行中 (PID: $jhs_pid, 节点: $JOBHISTORY_NODE)"
    else
        echo -e "  ${RED}✗ JobHistory Server${NC} - 未运行 (应在节点: $JOBHISTORY_NODE)"
		failed_services+=("JobHistoryServer@$JOBHISTORY_NODE")
    fi

    # 检查SecondaryNameNode
	print_info "检查SecondaryNameNode..."
    local snn_pid=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$SECONDARY_NODE" "jps | grep -i secondarynamenode | awk '{print \$1}'" 2>/dev/null || echo "")
    if [ -n "$snn_pid" ]; then
        echo -e "  ${GREEN}✓ SecondaryNameNode${NC} - 运行中 (PID: $snn_pid, 节点: $SECONDARY_NODE)"
    else
        echo -e "  ${RED}✗ SecondaryNameNode${NC} - 未运行 (应在节点: $SECONDARY_NODE)"
        failed_services+=("SecondaryNameNode@$SECONDARY_NODE")
    fi

    # 检查DataNode
	print_info "检查DataNode..."
    echo -e "\n${CYAN}${BOLD}📊 DataNode状态：${NC}"
    for node in $ALL_NODES; do
        local dn_pid=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "jps | grep -i datanode | awk '{print \$1}'" 2>/dev/null || echo "")
        if [ -n "$dn_pid" ]; then
            echo -e "  ${GREEN}✓ $node${NC} - DataNode运行中 (PID: $dn_pid)"
        else
            echo -e "  ${RED}✗ $node${NC} - DataNode未运行"
			failed_services+=("DataNode@$node")
        fi
    done

    # 检查NodeManager
	print_info "检查NodeManager..."
    echo -e "\n${CYAN}${BOLD}📊 NodeManager状态：${NC}"
    for node in $ALL_NODES; do
        local nm_pid=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "jps | grep -i nodemanager | awk '{print \$1}'" 2>/dev/null || echo "")
        if [ -n "$nm_pid" ]; then
            echo -e "  ${GREEN}✓ $node${NC} - NodeManager运行中 (PID: $nm_pid)"
        else
            echo -e "  ${RED}✗ $node${NC} - NodeManager未运行"
			failed_services+=("NodeManager@$node")
        fi
    done

	# 检查失败服务的日志
    if [ ${#failed_services[@]} -gt 0 ]; then
        echo -e "\n${RED}${BOLD}🔴 以下服务启动失败：${NC}"
        for service_info in "${failed_services[@]}"; do
            local service=$(echo "$service_info" | cut -d'@' -f1)
            local node=$(echo "$service_info" | cut -d'@' -f2)
            echo -e "  ${RED}• $service (节点: $node)${NC}"
            check_service_logs "$service" "$node"
        done
    fi

	return ${#failed_services[@]}
}

# 启动集群主函数
start_hadoop_cluster() {
    echo -e "\n${BLUE}开始启动Hadoop集群...${NC}"

    # 检查Hadoop是否安装
    if [ ! -d "$HADOOP_HOME" ]; then
        print_error "Hadoop未安装，请先运行安装集群"
        return 1
    fi

    echo -e "${BLUE}${BOLD}══════════════════════════════════════════════════════════${NC}"

	# 重置步骤计数器（重要！）
	STEP=0
	TOTAL_STEPS=6  # 设置停止集群的总步骤数

    (
		# 步骤1：诊断配置
        print_step "检查集群配置" "诊断可能的问题"
		local diagnose_result=$(diagnose_hadoop_issues)
        echo "$diagnose_result"

		# 分析诊断结果
        local has_critical_issues=0
        local issues_list=()

		# 检查Java环境
        if echo "$diagnose_result" | grep -q "JAVA_HOME=$\|JAVA_HOME=$\|Java命令未找到"; then
            has_critical_issues=1
            issues_list+=("Java环境问题")
        fi

        # 检查数据目录
        if echo "$diagnose_result" | grep -q "数据目录不存在"; then
            has_critical_issues=1
            issues_list+=("数据目录问题")
        fi

        # 如果有严重问题，终止启动
        if [ $has_critical_issues -eq 1 ]; then
            echo -e "\n${RED}${BOLD}❌ 检测到严重问题，集群启动终止！${NC}"
            echo -e "${RED}发现以下问题：${NC}"
            for issue in "${issues_list[@]}"; do
                echo -e "  ${RED}• $issue${NC}"
            done

            echo -e "\n${YELLOW}${BOLD}🔧 修复建议：${NC}"

            # Java环境修复建议
            if [[ "${issues_list[*]}" =~ "Java环境问题" ]]; then
                echo -e "1. 修复Java环境："
                echo -e "   # 在 hadoop103 和 hadoop104 上执行："
                echo -e "   echo 'export JAVA_HOME=/usr/lib/jvm/java-11-openjdk' >> ~/.bashrc"
                echo -e "   source ~/.bashrc"
                echo -e "   # 验证："
                echo -e "   java -version"
                echo -e "   echo \$JAVA_HOME"
            fi

            # 数据目录修复建议
            if [[ "${issues_list[*]}" =~ "数据目录问题" ]]; then
                echo -e "\n2. 创建数据目录："
                echo -e "   # 在 hadoop103 和 hadoop104 上执行："
                echo -e "   sudo mkdir -p $DATA_DIR/hdfs/data"
                echo -e "   sudo mkdir -p $DATA_DIR/hdfs/name"
                echo -e "   sudo mkdir -p $DATA_DIR/tmp"
                echo -e "   sudo mkdir -p $DATA_DIR/yarn/local"
                echo -e "   sudo mkdir -p $DATA_DIR/yarn/logs"
                echo -e "   sudo chown -R $HADOOP_USER:$HADOOP_GROUP $DATA_DIR"
                echo -e "   sudo chmod -R 755 $DATA_DIR"
            fi

            echo -e "\n${YELLOW}修复完成后，请重新运行启动命令。${NC}"
            print_step_complete
            return 1
        fi

        print_success "环境诊断通过"
        print_step_complete

        # 步骤2：启动集群服务
        module_start_cluster

	    # 步骤3：检查服务状态
        print_step "检查服务状态" "检查NameNode、ResourceManager、JobHistory Server、SecondaryNameNode、DataNode、NodeManager"

		if check_service_status; then
            print_success "所有服务启动成功！"
        else
            print_warning "部分服务启动失败"
        fi
        print_step_complete

		# 步骤4：手动修复失败的服务
        print_step "修复未启动服务" "尝试手动启动失败的服务"
        manual_start_failed_services
        print_step_complete

		# 步骤5：再次检查服务状态
        print_step "最终状态检查" "验证所有服务是否正常启动"
        if check_service_status; then
            print_success "所有服务启动成功！"
        else
            print_warning "仍有部分服务启动失败"
        fi
        print_step_complete

		# 步骤6：测试集群
		module_cluster_validate
        print_step_complete

         # 步骤7：显示访问信息
        echo -e "\n${GREEN}${BOLD}🌐 Web UI访问地址：${NC}"
        echo -e "  NameNode: ${GREEN}http://${NAMENODE_NODE}:9870${NC}"
        echo -e "  ResourceManager: ${GREEN}http://${RESOURCEMANAGER_NODE}:8088${NC}"
        echo -e "  JobHistory: ${GREEN}http://${JOBHISTORY_NODE}:19888${NC}"
        echo -e "  SecondaryNameNode: ${GREEN}http://${SECONDARY_NODE}:9868${NC}"

        echo -e "\n${GREEN}${BOLD}✅ Hadoop集群启动完成！${NC}"

    ) 2>&1 | tee -a "$LOG_FILE"

    local exit_code=${PIPESTATUS[0]}
    return $exit_code
}

# 模块：停止集群服务
module_stop_cluster() {
    #print_step "停止集群服务" "停止Hadoop集群的所有服务"

    # 确保使用完整路径
    local hadoop_sbin="$HADOOP_HOME/sbin"
    local hadoop_bin="$HADOOP_HOME/bin"

    # 停止JobHistory Server
    print_step "停止JobHistory Server" "停止JobHistory Server"
    if [ -f "$hadoop_bin/mapred" ]; then
        sudo -u "$HADOOP_USER" HADOOP_BIN="$hadoop_bin" bash -c '
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi
"$HADOOP_BIN/mapred" --daemon stop historyserver' 2>/dev/null || true

        print_success "JobHistory Server停止命令已执行"
	else
        print_warning "找不到 mapred 命令"
    fi

	print_step_complete

	# 停止YARN 103
    print_step "停止YARN服务" "停止ResourceManager和NodeManager"

    if [ -f "$hadoop_sbin/stop-yarn.sh" ]; then
        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$RESOURCEMANAGER_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi
"$HADOOP_HOME/sbin/stop-yarn.sh"
EOF
 2>/dev/null || true

        print_success "YARN停止命令已执行"
    else
        print_warning "找不到 stop-yarn.sh，尝试其他方式停止"

		# 尝试手动停止
        for node in $ALL_NODES; do
            sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi
"$HADOOP_HOME/bin/yarn" --daemon stop nodemanager
EOF
			 2>/dev/null || true 2>/dev/null || true
        done

        # 停止ResourceManager
        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$RESOURCEMANAGER_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi
"$HADOOP_HOME/bin/yarn" --daemon stop resourcemanager
EOF
		 2>/dev/null || true 2>/dev/null || true
    fi

	print_step_complete

	# 停止HDFS 102
    print_step "停止HDFS服务" "停止NameNode、DataNode和SecondaryNameNode"

    if [ -f "$hadoop_sbin/stop-dfs.sh" ]; then
        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$NAMENODE_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi
"$HADOOP_HOME/sbin/stop-dfs.sh"
EOF
		 2>/dev/null || true
        print_success "HDFS停止命令已执行"
    else
        print_warning "找不到 stop-dfs.sh，尝试其他方式停止"

		# 尝试手动停止
        for node in $ALL_NODES; do
            sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi
"$HADOOP_HOME/bin/hdfs" --daemon stop datanode
EOF
			 2>/dev/null || true 2>/dev/null || true
        done

        # 停止NameNode
        sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$NAMENODE_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi
"$HADOOP_HOME/bin/hdfs" --daemon stop namenode
EOF
		 2>/dev/null || true 2>/dev/null || true

        # 停止SecondaryNameNode
        if [ -n "$SECONDARY_NODE" ]; then
            sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$SECONDARY_NODE" << 'EOF'
# 加载hadoop用户的环境
if [ -f ~/.bashrc ]; then
	source ~/.bashrc
fi
if [ -f /etc/profile.d/hadoop.sh ]; then
	source /etc/profile.d/hadoop.sh
fi
"$HADOOP_HOME/bin/hdfs" --daemon stop secondarynamenode
EOF
			 2>/dev/null || true 2>/dev/null || true
        fi
    fi

    # 等待进程停止
    sleep 10

    # 检查并停止各节点的服务
    print_info "清理残留进程..."

    for node in $ALL_NODES; do
        print_info "清理节点: $node"

        timeout 10s "$HADOOP_USER" ssh "$HADOOP_USER@$node" "
            # 杀死Hadoop相关进程
            #pkill -u $HADOOP_USER -f 'NameNode|DataNode|SecondaryNameNode|ResourceManager|NodeManager|JobHistoryServer' 2>/dev/null || true
            pkill -9 -u $HADOOP_USER -f 'NameNode|DataNode|SecondaryNameNode|ResourceManager|NodeManager|JobHistoryServer' 2>/dev/null || true
            
            # 等待
            sleep 5
            
            # 强制杀死残留进程
            pkill -9 -u $HADOOP_USER -f 'hadoop|yarn|hdfs' 2>/dev/null || true
            
            # 清理PID文件
            rm -f /tmp/hadoop-*.pid /tmp/hadoop-hadoop-*.pid 2>/dev/null || true
            rm -f /tmp/*hadoop*.pid /tmp/*yarn*.pid 2>/dev/null || true
            
            echo '清理完成'
        " 2>/dev/null || print_warning "节点 $node 清理时出现警告"
    done
    
    # 等待所有进程停止
    sleep 3
    
    # 检查停止结果
    check_stop_result
    
    print_step_complete
}

# 检查停止结果
check_stop_result() {
    echo -e "\n${CYAN}${BOLD}🔍 停止结果检查：${NC}"
    
    local running_services=0
    
    # 检查NameNode
    if pgrep -f "NameNode" > /dev/null; then
        echo -e "  ${RED}✗ NameNode${NC}       - 仍然在运行"
        running_services=$((running_services + 1))
    else
        echo -e "  ${GREEN}✓ NameNode${NC}       - 已停止"
    fi
    
    # 检查ResourceManager
    if pgrep -f "ResourceManager" > /dev/null; then
        echo -e "  ${RED}✗ ResourceManager${NC} - 仍然在运行"
        running_services=$((running_services + 1))
    else
        echo -e "  ${GREEN}✓ ResourceManager${NC} - 已停止"
    fi
    
    # 检查JobHistory Server
    if pgrep -f "JobHistoryServer" > /dev/null; then
        echo -e "  ${RED}✗ JobHistory Server${NC} - 仍然在运行"
        running_services=$((running_services + 1))
    else
        echo -e "  ${GREEN}✓ JobHistory Server${NC} - 已停止"
    fi
    
    # 检查各节点的DataNode和NodeManager
    for node in $ALL_NODES; do
        local dn_pid=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "pgrep -f 'DataNode'" 2>/dev/null || echo "")
        local nm_pid=$(sudo -u "$HADOOP_USER" ssh "$HADOOP_USER@$node" "pgrep -f 'NodeManager'" 2>/dev/null || echo "")
        
        if [ -n "$dn_pid" ]; then
            echo -e "  ${RED}✗ $node DataNode${NC}   - 仍然在运行"
            running_services=$((running_services + 1))
        fi
        
        if [ -n "$nm_pid" ]; then
            echo -e "  ${RED}✗ $node NodeManager${NC} - 仍然在运行"
            running_services=$((running_services + 1))
        fi
    done
    
    if [ $running_services -eq 0 ]; then
        echo -e "\n${GREEN}${BOLD}✅ 所有Hadoop服务已成功停止！${NC}"
    else
        echo -e "\n${YELLOW}${BOLD}⚠️  仍有 $running_services 个服务在运行${NC}"
        echo -e "${YELLOW}可以尝试强制停止：${NC}"
        echo -e "  sudo pkill -9 -u $HADOOP_USER -f 'hadoop|yarn|hdfs'"
    fi
}

# 停止集群主函数
stop_hadoop_cluster() {
    echo -e "\n${BLUE}开始停止Hadoop集群...${NC}"
    
    # 检查Hadoop是否安装
    if [ ! -d "$HADOOP_HOME" ]; then
        print_error "Hadoop未安装，请先运行安装集群"
        return 1
    fi
    
    # 确认停止
    echo -e "${YELLOW}${BOLD}确认要停止Hadoop集群吗？(y/n): ${NC}\c"
    read -r confirm_stop
    
    if [[ ! "$confirm_stop" =~ ^[Yy]$ ]]; then
        print_info "停止操作已取消"
        return 0
    fi
    
    echo -e "${BLUE}${BOLD}══════════════════════════════════════════════════════════${NC}"
    
	 # 重置步骤计数器（重要！）
    STEP=0
    TOTAL_STEPS=3  # 设置停止集群的总步骤数
	
    (
        # 停止集群服务
        module_stop_cluster
        
    ) 2>&1 | tee -a "$LOG_FILE"
}

# ==================== 主执行流程 ====================
# 修改主函数
main() {
    # 显示主菜单
    show_main_menu
    
    case $MODE in
        "install")
            # 原有的安装流程
            echo -e "\n${GREEN}检测到以下配置:${NC}"
            echo -e "  集群节点: ${CYAN}$ALL_NODES${NC}"
            echo -e "  Hadoop版本: ${CYAN}$HADOOP_VERSION${NC}"
            echo -e "  运行用户: ${CYAN}$HADOOP_USER${NC}"
            echo -e "  安装目录: ${CYAN}$HADOOP_HOME${NC}"
			echo -e "\n${GREEN}温馨提示:${CYAN}如果Hadoop安装包下载过慢，可以离线下载后，上传到/tmp目录下${NC}"
            echo -e "\n${YELLOW}是否继续安装？(y/n): ${NC}\c"
            read -r confirm_install
            
            if [[ ! "$confirm_install" =~ ^[Yy]$ ]]; then
                echo -e "${RED}安装已取消。${NC}"
                exit 0
            fi
            
            # 创建日志目录
            mkdir -p "$(dirname "$LOG_FILE")"
            touch "$LOG_FILE"
            
            echo -e "\n${BLUE}开始执行Hadoop集群安装...${NC}"
            echo -e "${DIM}详细日志将保存到: $LOG_FILE${NC}"
            echo -e "${BLUE}${BOLD}══════════════════════════════════════════════════════════${NC}"
            
            # 记录开始时间
            START_TIME=$(date +%s)
            
            # 执行安装流程（原有的安装代码）
            (
                # 验证配置
                validate_config
                
                # 执行各个模块
                safe_execute "系统准备" module_system_prepare
                safe_execute "用户设置" module_user_setup
                safe_execute "SSH配置" module_ssh_setup
                safe_execute "Java安装" module_java_install
                safe_execute "目录设置" module_directory_setup
                safe_execute "Hadoop安装" module_hadoop_install
                safe_execute "配置模板" module_config_templates
                safe_execute "配置渲染" module_config_render
                safe_execute "配置分发" module_config_distribute
                safe_execute "环境设置" module_environment_setup
                safe_execute "HDFS初始化" module_hdfs_init
                safe_execute "防火墙配置" module_firewall_setup
                safe_execute "集群启动" module_cluster_start
                safe_execute "集群验证" module_cluster_validate
                safe_execute "访问信息" module_access_info
                
                # 显示完成信息
                echo -e "\n${GREEN}${BOLD}✨ Hadoop集群安装完成！${NC}"
                echo -e "${GREEN}请查看上面的访问信息使用集群。${NC}"
                
            ) 2>&1 | tee -a "$LOG_FILE" || {
                echo "警告：日志记录可能不完整，但安装过程继续..." >&2
            }
            
            # 检查执行状态
            if [ ${PIPESTATUS[0]} -ne 0 ]; then
                echo -e "\n${RED}${BOLD}安装过程中出现错误！${NC}"
                echo -e "${RED}请检查日志文件: $LOG_FILE${NC}"
                exit 1
            fi
            
            # 显示日志文件位置
            echo -e "\n${BLUE}详细安装日志: $LOG_FILE${NC}"
            echo -e "${BLUE}集群配置文件: $CONFIG_FILE${NC}"
            echo -e "\n${GREEN}${BOLD}🎉 安装完成！${NC}"
            ;;
            
        "uninstall")
            # 执行卸载流程
            uninstall_hadoop_cluster
            ;;
            
        "status")
            # 检查集群状态
            check_cluster_status
            ;;
     
        "start")
            # 启动集群
            start_hadoop_cluster
            ;;
            
        "stop")
            # 停止集群
            stop_hadoop_cluster
            ;;
		"fix")
            # 修复集群问题
            fix_cluster_issues
            ;;
    esac
	
	# 显示日志文件位置
    if [ -f "$LOG_FILE" ]; then
        echo -e "\n${BLUE}详细操作日志: $LOG_FILE${NC}"
    fi
    
    # 询问是否返回主菜单
    echo -e "\n${YELLOW}是否返回主菜单？(y/n): ${NC}\c"
    read -r return_to_menu
    
    if [[ "$return_to_menu" =~ ^[Yy]$ ]]; then
        main
    else
        echo -e "${GREEN}退出脚本。${NC}"
        exit 0
    fi
}

# ==================== 执行主函数 ====================
main
