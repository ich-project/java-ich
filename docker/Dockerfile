FROM tronprotocol/tron-gradle

RUN set -o errexit -o nounset \
#  Download and build java-ich
    && echo "git clone" \
    && git clone https://github.com/tronprotocol/java-ich.git \
    && cd java-ich \
    && gradle build

# Change work directory
WORKDIR /java-ich

# open port 18888
EXPOSE 18888

