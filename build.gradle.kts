plugins {
    id("java")
    id("application")
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://dl.cloudsmith.io/public/clawsoftstudios/purffectlib/maven/")
    }
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.2.7")
    implementation("org.yaml:snakeyaml:2.0")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.clawsoftstudios.purrfectlib:javautils:0.0.6")
    implementation("org.mongojack:mongojack:5.0.2")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testCompileOnly("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")
}



application {
    mainClass = "com.example.discordbot.Bot"
}