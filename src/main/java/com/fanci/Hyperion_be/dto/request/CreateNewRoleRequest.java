package com.fanci.Hyperion_be.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNewRoleRequest {
    private String roleName;
    private List<Long> permissionId;
}