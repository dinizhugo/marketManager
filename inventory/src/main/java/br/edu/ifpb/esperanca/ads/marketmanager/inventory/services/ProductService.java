package br.edu.ifpb.esperanca.ads.marketmanager.inventory.services;

import br.edu.ifpb.esperanca.ads.marketmanager.inventory.dtos.ProductRequestDTO;
import br.edu.ifpb.esperanca.ads.marketmanager.inventory.dtos.ProductResponseDTO;
import br.edu.ifpb.esperanca.ads.marketmanager.inventory.mappers.ProductMapper;
import br.edu.ifpb.esperanca.ads.marketmanager.inventory.models.Product;
import br.edu.ifpb.esperanca.ads.marketmanager.inventory.repositories.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final SupplierService supplierService;
    private final ReceivingService receivingService;
    private final static Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductMapper productMapper;

    @Autowired
    public ProductService(ProductRepository productRepository, SupplierService supplierService, ReceivingService receivingService, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.supplierService = supplierService;
        this.receivingService = receivingService;
        this.productMapper = productMapper;
    }

    public ProductResponseDTO registerNewProduct(ProductRequestDTO dto) {
        log.info("Registering a new product...");
        validateProduct(dto);

        Product product = productMapper.toEntity(dto);
        log.info("Product successfully registered!");

        productRepository.save(product);
        return productMapper.toProductResponseDTO(product);
    }

    public ProductResponseDTO getProductById(Long id) {
        log.info("Searching for product with id: {}", id);
        var product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Product not found"));
        log.info("Product found successfully.");
        return productMapper.toProductResponseDTO(product);
    }

    public List<ProductResponseDTO> getAllProducts() {
        log.info("Returning all products from the database.");
        return productRepository.findAll()
                .stream()
                .map(productMapper::toProductResponseDTO)
                .collect(Collectors.toList());
    }

    public void deleteProduct(Long id) {
        log.info("Deleting product...");
        var product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Product not found"));

        log.info("Product successfully deleted.");
        productRepository.delete(product);
    }

    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto) {
        log.info("Updating product with id: {}", id);
        var existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Product not found"));

        validateProduct(dto);

        Product updatedProduct = productMapper.toEntity(dto);
        updatedProduct.setId(existingProduct.getId());

        log.info("Product successfully updated!");
        productRepository.save(updatedProduct);
        return productMapper.toProductResponseDTO(updatedProduct);
    }

    public void updateStockQuantity(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getAvailableQuantity() - quantity < 0) {
            throw new RuntimeException("Quantidade Insuficiente");
        }

        product.setAvailableQuantity(product.getAvailableQuantity() - quantity);
        productRepository.save(product);
    }

    private void validateProduct(ProductRequestDTO dto) {
        if (dto.availableQuantity() < 1 && dto.totalQuantity() < 1) {
            log.warn("Attempt to register a product with quantities less than or equal to 0");
            throw new RuntimeException("Cannot register a product with quantities less than or equal to 0");
        }

        if (!dto.availableQuantity().equals(dto.totalQuantity())) {
            log.warn("Attempt to register a product where total and available quantities are different");
            throw new RuntimeException("Cannot register a product where the total and available quantities are different");
        }

        if (supplierService.getSupplierById(dto.supplierId().getId()) == null) {
            log.warn("Attempt to create a product with a non-existent supplier. Supplier ID: {}", dto.supplierId().getId());
            throw new RuntimeException("Cannot create a product without a valid supplier ID.");
        }

        if (receivingService.getReceivingById(dto.receivingId().getId()) == null) {
            log.warn("Attempt to create a product with a non-existent receiving record. Receiving ID: {}", dto.receivingId().getId());
            throw new RuntimeException("Cannot create a product without a valid receiving ID.");
        }
    }
}