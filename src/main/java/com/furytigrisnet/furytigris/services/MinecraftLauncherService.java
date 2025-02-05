package com.furytigrisnet.furytigris.services;

import com.furytigrisnet.furytigris.records.MinecraftParams;
import com.furytigrisnet.furytigris.util.OSHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Serviço para gerenciar as operações do launcher do Minecraft.
 * Combina funcionalidades de download de arquivos, extração e execução.
 *
 * @author Anderson Andrade Dev
 * @Data de Criação 09/01/2025
 */
@Service
public class MinecraftLauncherService {

    private static final Logger logger = LoggerFactory.getLogger(MinecraftLauncherService.class);

    // Caminho base para os arquivos baixados
    private final String basePath = System.getProperty("user.dir") + File.separator + "downloads";

    /**
     * Inicia o Minecraft com os parâmetros especificados, executando o comando gerado.
     *
     * @throws IllegalStateException Se o comando gerado for inválido ou não puder ser executado.
     */
    public void launchMinecraft() {

        // Caminho para o arquivo JAR do Minecraft
        File jar = new File(basePath + File.separator + "minecraft.jar");

        // Verificando se o arquivo JAR existe antes de tentar iniciar o Minecraft
        if (!jar.exists()) {
            throw new IllegalStateException("O arquivo JAR do Minecraft não foi encontrado: " + jar.getAbsolutePath());
        }

        // Construindo o comando para iniciar o Minecraft com os parâmetros fornecidos
        String command = buildMinecraftCommand(jar);

        // Verificando se o comando foi gerado corretamente
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalStateException("O comando para iniciar o Minecraft é inválido.");
        }

        try {
            // Logando o comando antes de executá-lo
            logger.info("Executando comando: {}", command);

            // Usando ProcessBuilder para garantir controle adequado sobre o ambiente
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", command);
            processBuilder.environment().put("JAVA_HOME", "/usr/lib/jvm/java-8-openjdk-amd64");

            Process process = processBuilder.start();

            logProcessOutput(process);
        } catch (IOException e) {
            // Capturando e logando erros ao tentar iniciar o Minecraft
            logger.error("Erro ao iniciar o Minecraft: {}", e.getMessage(), e);
            throw new IllegalStateException("Erro ao iniciar o Minecraft: " + e.getMessage(), e);
        }
    }


    /**
     * Constrói o comando para rodar o cliente Minecraft utilizando os parâmetros fornecidos.
     *
     * @param jar Arquivo .jar que será utilizado para o cliente Minecraft.
     * @return String contendo o comando para execução do Minecraft.
     * @throws IllegalArgumentException Se algum dos parâmetros obrigatórios não for válido.
     */
    private String buildMinecraftCommand(File jar) {
        // Verificando se o arquivo JAR está válido
        if (jar == null || !jar.exists()) {
            throw new IllegalArgumentException("O arquivo JAR fornecido é inválido ou não existe.");
        }

        // Definindo os diretórios necessários para os arquivos nativos e bibliotecas
        var urlNatives = basePath + File.separator + "natives";
        var urlLibraries = basePath + File.separator + "libraries";

        // Verificando se os diretórios necessários existem
        if (!new File(urlNatives).exists()) {
            throw new IllegalArgumentException("O diretório para arquivos nativos não foi encontrado: " + urlNatives);
        }
        if (!new File(urlLibraries).exists()) {
            throw new IllegalArgumentException("O diretório para arquivos de bibliotecas não foi encontrado: " + urlLibraries);
        }

        // Definindo parâmetros de memória
        var memoriaMim = "1024M";
        var memoriaMax = "4096M";

        // Armazenando a client e contatenar todas bibliotecas encontrado em librarias
        StringBuilder classpath = new StringBuilder(jar.getAbsolutePath());
        File librariesDir = new File(urlLibraries);
        File[] libraryFiles = librariesDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (libraryFiles != null) {
            for (File library : libraryFiles) {
                classpath.append(File.pathSeparator).append(library.getAbsolutePath());
            }
        } else {
            System.err.println("Nenhuma biblioteca encontrada no diretório: " + urlLibraries);
        }

        return String.format(
                "java -Xms%s -Xmx%s -Djava.library.path=\"%s\" -cp \"%s\" net.minecraft.client.main.Main " +
                        "--accessToken %s --version %s --gameDir \"%s\" --assetsDir \"%s\" --assetsIndex %s --width %s --height %s --username %s --userType %s",
                memoriaMim, memoriaMax, urlNatives, classpath,
                "000", "1.8.8", OSHelper.getOS().getMc(),
                OSHelper.getOS().getMc() + File.separator + "assets",
                "1.8.8", "854", "480", "_ZinhoZin", "mojang"
        );
//        // Criando o objeto MinecraftParams
//        var params = new MinecraftParams(
//                memoriaMim,
//                memoriaMax,
//                urlNatives,
//                classPath,
//                jar.getAbsolutePath(),
//                OSHelper.getOS().getMc(),
//                OSHelper.getOS().getMc() + File.separator + "assets",
//                "854", // Largura da tela
//                "480", // Altura da tela
//                "_ZinhoZin", // Nome do usuário
//                "1.8.8", // Versão do Minecraft
//                "1.8.8", // Índice de recursos do Minecraft
//                "N/A", // UUID do usuário
//                "000", // Token de acesso
//                "mojang" // Tipo de usuário
//        );

        // Retornando o comando formatado
//        return params.buildMinecraftCommand();
    }

    /**
     * Registra a saída e os erros de um processo fornecido.
     *
     * @param process O processo para registrar.
     * @throws IOException Se ocorrer um erro durante a leitura.
     */
    private void logProcessOutput(Process process) throws IOException {
        try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            String line;
            while ((line = stdInput.readLine()) != null) {
                logger.info(line);
            }
            while ((line = stdError.readLine()) != null) {
                logger.error(line);
            }
        }
    }
}

