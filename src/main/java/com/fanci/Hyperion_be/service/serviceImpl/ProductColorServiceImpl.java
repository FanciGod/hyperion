package com.fanci.Hyperion_be.service.serviceImpl;

import com.fanci.Hyperion_be.dto.request.CreateNewProductColorRequest;
import com.fanci.Hyperion_be.dto.response.ProductColorResponse;
import com.fanci.Hyperion_be.mapper.ProductColorMapper;
import com.fanci.Hyperion_be.repository.ProductColorRepository;
import com.fanci.Hyperion_be.service.ProductColorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
@RequiredArgsConstructor
@Service
public class ProductColorServiceImpl implements ProductColorService {
    private final ProductColorRepository productColorRepository;
    private final ProductColorMapper productColorMapper;
    @Override
    public List<ProductColorResponse> getAllColors() {
        return productColorRepository.findAll().stream().map(productColorMapper::toProductColorResponse).toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SALE')")
    public ProductColorResponse addNewColor(CreateNewProductColorRequest request) {
        var productColor = productColorMapper.toProductColor(request);
        return productColorMapper.toProductColorResponse(productColorRepository.save(productColor));
    }
}
