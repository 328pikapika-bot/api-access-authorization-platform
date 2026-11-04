package com.nstl.openplatform.dto;

import lombok.Data;

@Data
public class NstlSessionRequest {

    private String userId;
    private String username;
    private String nickname;
    private String email;
    private String loginType;
    private String nstlUnionId;
    private String ssoId;
    private String phone;
    private String unitName;
}
