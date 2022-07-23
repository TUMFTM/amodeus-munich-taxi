FROM amazoncorretto:11 as builder

# Install prerequisites for the GLPK library
RUN yum update -y && \
 yum install -y gcc swig wget make git svn tar

# Install the GLPK library
RUN wget ftp://ftp.gnu.org/gnu/glpk/glpk-4.65.tar.gz && \
    tar -xzf glpk-4.65.tar.gz && \
    rm glpk-4.65.tar.gz && \
    cd glpk-4.65 && \
    ./configure && \
    make && \
    make check && \
    make install && \
    ldconfig

# Install the Java interface to the GLPK library
RUN wget https://netix.dl.sourceforge.net/project/glpk-java/glpk-java/glpk-java-1.12.0/libglpk-java-1.12.0.tar.gz && \
    tar -xzf libglpk-java-1.12.0.tar.gz && \
    rm libglpk-java-1.12.0.tar.gz

# Update pom.xml, so javadoc generation does not fail (https://stackoverflow.com/a/60322437/10608063)[pom.xml](uploads/63fd3f769333704ef89dcc3753b6396d/pom.xml)
COPY docker-utils/glpk.pom.xml libglpk-java-1.12.0/swig/pom.xml

RUN cd libglpk-java-1.12.0 && \
    ./configure LDFLAGS=-L/usr/local/lib && \
    make && \
    make check && \
    make install && \
    ldconfig

# Install Gurobi inside the docker container
RUN wget https://packages.gurobi.com/9.1/gurobi9.1.2_linux64.tar.gz && \
    tar -xzf gurobi9.1.2_linux64.tar.gz && \
    rm gurobi9.1.2_linux64.tar.gz

# Set GUROBI_HOME environment variable
ENV GUROBI_HOME=/gurobi902/linux64

# Copy license file for getting a floating license from the host
COPY docker-utils/gurobi.lic /opt/gurobi/gurobi.lic

ENV LD_LIBRARY_PATH=/usr/local/lib/jni:/usr/lib/x86_64-linux-gnu/jni:/gurobi902/linux64/lib

RUN  yum clean all && rm -rf /var/cache/yum

ENV TZ=Europe/Berlin
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

FROM builder

COPY ./target/amod.jar amod.jar

ENTRYPOINT ["java", "-Dmatsim.preferLocalDtds=true", "-Djava.awt.headless=true"]

CMD ["-Xmx30000m", "-cp", "/amod.jar", "de.tum.mw.ftm.amod.taxi.ScenarioExecutionSequence"]