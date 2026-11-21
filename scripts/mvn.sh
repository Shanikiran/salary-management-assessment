#!/usr/bin/env bash
# Wrapper for Maven in this sandbox: the egress proxy credentials rotate
# on every shell invocation, so settings.xml must be regenerated from the
# CURRENT environment before each Maven run. Also pins the Google Maven
# Central mirror (plain HTTP, no TLS tunnel needed) and the local JDK/Maven.
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-$HOME/workspace/tools/jdk-17.0.21.1+2}"
# The proxy hostname resolves to both an IPv4 gateway (198.19.0.1, the real
# proxy) and an IPv6 address that returns garbage to Java's HTTP stack.
# Force IPv4 so Maven talks to the real proxy.
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djava.net.preferIPv4Stack=true"
MVN="$HOME/workspace/tools/apache-maven-3.9.9/bin/mvn"

PROXY_URL="${https_proxy:-${http_proxy:-}}"
if [ -n "$PROXY_URL" ]; then
  PROXY_USER="$(echo "$PROXY_URL" | sed -E 's|.*://([^:]+):([^@]+)@.*|\1|')"
  PROXY_PASS="$(echo "$PROXY_URL" | sed -E 's|.*://[^:]+:([^@]+)@.*|\1|')"
  PROXY_HOST="$(echo "$PROXY_URL" | sed -E 's|.*@([^:]+):([0-9]+)|\1|')"
  PROXY_PORT="$(echo "$PROXY_URL" | sed -E 's|.*@([^:]+):([0-9]+)|\2|')"
  SETTINGS=$(cat <<EOF
<settings>
  <mirrors>
    <mirror>
      <id>google-maven-central</id>
      <mirrorOf>central</mirrorOf>
      <url>http://maven-central.storage-download.googleapis.com/maven2</url>
    </mirror>
  </mirrors>
  <proxies>
    <proxy>
      <id>egress</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>${PROXY_HOST}</host>
      <port>${PROXY_PORT}</port>
      <username>${PROXY_USER}</username>
      <password>${PROXY_PASS}</password>
      <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
    </proxy>
    <proxy>
      <id>egress-https</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>${PROXY_HOST}</host>
      <port>${PROXY_PORT}</port>
      <username>${PROXY_USER}</username>
      <password>${PROXY_PASS}</password>
      <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
EOF
)
  for d in /root/.m2 /home/hatch/.m2; do
    mkdir -p "$d"
    printf '%s\n' "$SETTINGS" > "$d/settings.xml"
    chmod 600 "$d/settings.xml"
  done
fi

exec "$MVN" "$@"
