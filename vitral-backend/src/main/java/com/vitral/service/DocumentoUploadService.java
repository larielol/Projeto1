package com.vitral.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.entity.Account;
import com.vitral.entity.DocumentoUpload;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.repository.DocumentoUploadRepository;
import com.vitral.repository.SeboRepository;

@Service
public class DocumentoUploadService {
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "application/pdf", "image/jpeg", "image/png");
    private static final Map<String, String> EXTENSOES = Map.of(
            "application/pdf", ".pdf", "image/jpeg", ".jpg", "image/png", ".png");

    private final Path diretorio;
    private final long tamanhoMaximo;
    private final SeboRepository seboRepository;
    private final DocumentoUploadRepository metadataRepository;

    public DocumentoUploadService(
            @Value("${app.upload.document-dir:uploads/documents}") String diretorio,
            @Value("${app.upload.document-max-size-bytes:10485760}") long tamanhoMaximo,
            SeboRepository seboRepository,
            DocumentoUploadRepository metadataRepository) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
        this.tamanhoMaximo = tamanhoMaximo;
        this.seboRepository = seboRepository;
        this.metadataRepository = metadataRepository;
    }

    @Transactional
    public String armazenar(Account account, MultipartFile arquivo) {
        return arquivoUrl(armazenarComMetadata(account, arquivo));
    }

    @Transactional
    public DocumentoUpload armazenarComMetadata(Account account, MultipartFile arquivo) {
        String tipo = validar(arquivo);
        Sebo sebo = seboRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sebo nao encontrado para esta conta"));
        try {
            Files.createDirectories(diretorio);
            String nome = UUID.randomUUID() + EXTENSOES.get(tipo);
            Path destino = diretorio.resolve(nome).normalize();
            if (!destino.startsWith(diretorio)) throw erro("Nome de arquivo invalido.");
            try (InputStream input = arquivo.getInputStream()) {
                Files.copy(input, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            DocumentoUpload metadata = DocumentoUpload.builder().sebo(sebo).nomeInterno(nome)
                    .nomeOriginal(nomeOriginal(arquivo)).mimeType(tipo).tamanhoBytes(arquivo.getSize()).build();
            DocumentoUpload salvo = metadataRepository.save(metadata);
            return salvo == null ? metadata : salvo;
        } catch (IOException e) {
            throw erro("Nao foi possivel salvar o documento.");
        }
    }

    public String arquivoUrl(DocumentoUpload metadata) {
        return "/api/v1/uploads/documents/" + metadata.getNomeInterno();
    }

    @Transactional(readOnly = true)
    public Resource carregar(Account account, String nome) {
        DocumentoUpload metadata = metadataRepository.findByNomeInterno(nome)
                .orElseThrow(() -> new ResourceNotFoundException("Documento nao encontrado."));
        boolean admin = account.getType() == AccountType.ADMIN;
        boolean proprietario = metadata.getSebo().getAccount().getId().equals(account.getId());
        if (!admin && !proprietario) {
            throw new BusinessException("Acesso negado ao documento", HttpStatus.FORBIDDEN);
        }
        try {
            Path arquivo = diretorio.resolve(nome).normalize();
            if (!arquivo.startsWith(diretorio) || !Files.isRegularFile(arquivo)) {
                throw new ResourceNotFoundException("Documento nao encontrado.");
            }
            return new UrlResource(arquivo.toUri());
        } catch (IOException e) {
            throw new ResourceNotFoundException("Documento nao encontrado.");
        }
    }

    public String contentType(String nome) {
        String n = nome.toLowerCase(Locale.ROOT);
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    private String validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) throw erro("Selecione um documento para upload.");
        if (arquivo.getSize() > tamanhoMaximo) throw erro("O documento excede o tamanho maximo permitido.");
        if (!TIPOS_PERMITIDOS.contains(arquivo.getContentType())) {
            throw erro("Formato invalido. Use PDF, JPG, JPEG ou PNG.");
        }
        validarExtensao(arquivo.getOriginalFilename(), arquivo.getContentType());
        try {
            byte[] inicio = arquivo.getInputStream().readNBytes(12);
            String detectado = detectarTipo(inicio);
            if (!arquivo.getContentType().equals(detectado)) {
                throw erro("O conteudo do arquivo nao corresponde ao formato informado.");
            }
            return detectado;
        } catch (IOException e) {
            throw erro("Nao foi possivel validar o documento.");
        }
    }

    private String detectarTipo(byte[] b) {
        if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') return "application/pdf";
        if (b.length >= 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff) return "image/jpeg";
        if (b.length >= 8 && (b[0] & 0xff) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0d && b[5] == 0x0a && b[6] == 0x1a && b[7] == 0x0a) return "image/png";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        throw erro("Conteudo de arquivo invalido. Use PDF, JPG, JPEG ou PNG.");
    }

    private void validarExtensao(String nome, String tipo) {
        String n = nome == null ? "" : nome.toLowerCase(Locale.ROOT);
        boolean valida = switch (tipo) {
            case "application/pdf" -> n.endsWith(".pdf");
            case "image/jpeg" -> n.endsWith(".jpg") || n.endsWith(".jpeg");
            case "image/png" -> n.endsWith(".png");
            default -> false;
        };
        if (!valida) throw erro("A extensao do arquivo nao corresponde ao formato informado.");
    }

    private String nomeOriginal(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        if (nome == null || nome.isBlank()) return "documento" + EXTENSOES.get(arquivo.getContentType());
        return Path.of(nome).getFileName().toString();
    }

    private BusinessException erro(String mensagem) {
        return new BusinessException(mensagem, HttpStatus.BAD_REQUEST);
    }
}
