package com.jsbs.casemall.service;

import com.jsbs.casemall.constant.ProductCategory;
import com.jsbs.casemall.constant.ProductType;
import com.jsbs.casemall.dto.*;
import com.jsbs.casemall.entity.Product;
import com.jsbs.casemall.entity.ProductImg;
import com.jsbs.casemall.entity.ProductModel;
import com.jsbs.casemall.repository.ProductImgRepository;
import com.jsbs.casemall.repository.ProductModelRepository;
import com.jsbs.casemall.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductImgRepository productImgRepository;
    private final ProductRepository productRepository;
    private final ProductImgService productImgService;
    private final ProductModelRepository productModelRepository;

    public Long saveProduct(ProductFormDto productFormDto, List<MultipartFile> productImgFileList) throws Exception {
        log.info("상품 등록 시작: {}", productFormDto);

        // 유효한 모델만 필터링
        List<ProductModelDto> validModels = productFormDto.getProductModelDtoList().stream()
                .filter(modelDto -> modelDto.getPrStock() != null && modelDto.getPrStock() > 0
                        && modelDto.getProductModelSelect() != null)
                .collect(Collectors.toList());

        // 유효한 모델이 없는 경우 예외 처리
        if (validModels.isEmpty()) {
            log.error("유효한 모델이 없습니다. 상품을 저장할 수 없습니다.");
            throw new Exception("유효한 모델이 없습니다. 상품을 저장할 수 없습니다.");
        }

        // 상품 등록
        Product product = productFormDto.createProduct();
        product = productRepository.save(product);  // 여기서 저장
        productRepository.flush();  // 여기서 flush 호출하여 pr_id를 생성

        log.info("상품 저장 완료: {}", product);

        // 모델 저장
        for (ProductModelDto modelDto : validModels) {
            ProductModel productModel = new ProductModel();
            productModel.setProductModelSelect(modelDto.getProductModelSelect());
            productModel.setPrStock(modelDto.getPrStock());
            product.addProductModel(productModel); // 유효한 모델만 product에 추가
            log.info("유효한 상품 모델 추가 완료: {}", productModel);
        }

        // 이미지 등록
        for (int i = 0; i < productImgFileList.size(); i++) {
            ProductImg productImg = new ProductImg();
            productImg.setProduct(product);

            if (i == 0) {
                productImg.setPrMainImg("Y");
            } else {
                productImg.setPrMainImg("N");
            }
            productImgService.saveProductImg(productImg, productImgFileList.get(i));
        }

        log.info("상품 이미지 저장 완료");

        return product.getId();
    }

    @Transactional(readOnly = true)
    public ProductFormDto getProductDtl(Long prId) {
        Product product = productRepository.findById(prId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        ProductFormDto productFormDto = ProductFormDto.of(product);

        List<ProductImg> productImgList = productImgRepository.findByIdOrderByIdAsc(prId);
        if (productImgList.isEmpty()) {
            log.warn("No product images found for ID: {}", prId);
        }

        List<ProductImgDto> productImgDtoList = product.getProductImgList().stream()
                .map(img -> {
                    ProductImgDto productImgDto = new ProductImgDto();
                    productImgDto.setImgUrl(img.getImgUrl());
                    return productImgDto;
                })
                .collect(Collectors.toList());

        productFormDto.setProductImgDtoList(productImgDtoList);

        return productFormDto;
    }

    public void updateProduct(ProductFormDto productFormDto, List<MultipartFile> productImgFileList) throws Exception {
        // 상품 엔티티 가져오기
        Product product = productRepository.findById(productFormDto.getId())
                .orElseThrow(EntityNotFoundException::new);

        // 상품 정보 업데이트
        product.updateProduct(productFormDto);

        // 상품 이미지가 비어있을 경우 처리
        if (productImgFileList.isEmpty()) {
            throw new IllegalArgumentException("Product image list cannot be empty");
        }

        // 상품 이미지 업데이트
        for (int i = 0; i < productImgFileList.size(); i++) {
            MultipartFile productImgFile = productImgFileList.get(i);
        }

        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<Product> getAdminProductPage(ProductSearchDto productSearchDto, Pageable pageable) {
        log.info("Fetching products for admin page with pageable: {}", pageable);
        return productRepository.getAdminProductPage(productSearchDto, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MainProductDto> getMainProductPage(ProductSearchDto productSearchDto, Pageable pageable) {
        return productRepository.getMainProductPage(productSearchDto, pageable);
    }

    @Transactional
    public void deleteProduct(Long prId) throws Exception {
        Product product = productRepository.findById(prId)
                .orElseThrow(() -> new EntityNotFoundException("상품이 존재하지 않습니다."));

        List<ProductImg> productImgList = productImgRepository.findByProductId(prId);

        // 상품에 연관된 이미지 삭제
        for (ProductImg productImg : productImgList) {
            productImgService.deleteProductImg(productImg.getId());
        }

        // 상품 삭제
        productRepository.delete(product);
    }

    public List<Product> getProductsByCategory(ProductCategory category) {
        return productRepository.findByProductCategory(category);
    }

    public List<Product> getProductsByType(ProductType type) {
        return productRepository.findByProductType(type);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public List<ProductModelDto> getProductModelsByProductId(Long productId) {
        List<ProductModel> productModels = productModelRepository.findByProductId(productId);
        return productModels.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ProductModelDto convertToDto(ProductModel productModel) {
        ProductModelDto productModelDto = new ProductModelDto();
        productModelDto.setId(productModel.getId());
        productModelDto.setProductModelSelect(productModel.getProductModelSelect());
        productModelDto.setPrStock(productModel.getPrStock());
        return productModelDto;
    }
}
