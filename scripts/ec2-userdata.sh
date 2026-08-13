#!/bin/bash
# EC2(Ubuntu) 최초 부팅 시 1회 실행되는 User Data 스크립트.
# 인스턴스 생성 시 "고급 세부 정보 > 사용자 데이터"에 이 내용을 붙여넣으면
# JDK21 + MySQL + Nginx + Certbot + swap(2GB)이 자동 설치된다.
# (시크릿은 포함하지 않는다 - MySQL DB/유저 생성과 배포는 이후 단계에서)
set -euxo pipefail
export DEBIAN_FRONTEND=noninteractive

# 1) swap 2GB (Spring Boot + MySQL 동시 구동 시 1GB RAM OOM 방지)
if [ ! -f /swapfile ]; then
  dd if=/dev/zero of=/swapfile bs=128M count=16
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# 2) 패키지 설치
apt-get update -y
apt-get install -y openjdk-21-jdk mysql-server nginx certbot python3-certbot-nginx

# 3) 서비스 기동
systemctl enable --now mysql
systemctl enable --now nginx

# 4) 앱 디렉토리 준비 (배포 산출물이 여기로 전송됨)
mkdir -p /home/ubuntu/app
chown -R ubuntu:ubuntu /home/ubuntu/app

echo "bootstrap done: jdk21 + mysql + nginx + certbot + swap(2GB)"
