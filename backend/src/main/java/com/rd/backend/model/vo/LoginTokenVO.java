package com.rd.backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginTokenVO implements Serializable {
    private String token;
    private LoginUserVO userInfo;
}
