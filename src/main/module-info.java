module matt.mstruct {


    requires kotlin.stdlib.jdk8;
    requires kotlin.stdlib.jdk7;
    requires kotlin.reflect;


    requires org.yaml.snakeyaml;

    requires matt.kjlib;
    exports matt.mstruct;
}