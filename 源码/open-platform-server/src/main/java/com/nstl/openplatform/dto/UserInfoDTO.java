package com.nstl.openplatform.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 用户信息DTO（NSTL格式）
 *
 * NSTL返回格式:
 * {
 *   "id": "1pniJ/6V lZ/1Ri00hPamQ==",
 *   "attributes": [
 *     {"external": ""},
 *     {"sourcelist": ""},
 *     {"unionid": "337f7d3693bdf36750c22f1a2d147df3"},
 *     {"mobilephone": "1565262000"},
 *     {"workunit": ""},
 *     {"usertype": "普通用户"},
 *     {"userid": "rkezqgsvegkpnxfe"},
 *     {"userlibcode": "CN000000"},
 *     {"email": "terryss@qq.com"},
 *     {"username": "王先生"},
 *     {"expire": 27605}
 *   ]
 * }
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
public class UserInfoDTO {

    /**
     * 统一认证唯一标识
     */
    private String id;

    /**
     * 用户属性列表
     */
    private List<Map<String, Object>> attributes;

    // 辅助方法：从attributes中获取指定key的值
    public String getAttributeValue(String key) {
        if (attributes == null) {
            return null;
        }
        for (Map<String, Object> attr : attributes) {
            if (attr.containsKey(key)) {
                Object value = attr.get(key);
                return value != null ? value.toString() : null;
            }
            for (Map.Entry<String, Object> entry : attr.entrySet()) {
                if (entry.getKey() != null && key.equalsIgnoreCase(entry.getKey())) {
                    Object value = entry.getValue();
                    return value != null ? value.toString() : null;
                }
            }
        }
        return null;
    }

    private String getFirstAttributeValue(String... keys) {
        for (String key : keys) {
            String value = getAttributeValue(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    // 方便的getter方法
    public String getSsoId() {
        return id;
    }

    public String getUserName() {
        return getFirstAttributeValue("userid", "userId", "user_id", "loginId", "loginName", "account");
    }

    public String getNickname() {
        return getFirstAttributeValue("username", "userName", "name", "realName", "nickName", "displayName");
    }

    public String getEmail() {
        return getFirstAttributeValue("email", "mail");
    }

    public String getPhone() {
        return getFirstAttributeValue("mobilephone", "mobilePhone", "mobile_phone", "mobile", "phone", "tel");
    }

    public String getUnitName() {
        return getFirstAttributeValue("workunit", "workUnit", "unitName", "unit", "orgName", "organizationName");
    }

    public String getUnionId() {
        return getFirstAttributeValue("unionid", "unionId", "union_id", "nstlUnionId", "nstl_union_id");
    }
}
