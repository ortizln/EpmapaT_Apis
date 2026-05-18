package com.erp.sri_files.models;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.*;
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement
public class YourDataModel {
    private String field1;
    private int field2;
    private List<String> items;

}