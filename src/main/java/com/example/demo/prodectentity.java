package com.example.demo;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "product_table")
public class prodectentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name")
    private String productName;

    private double price;

    private String material;

    @Column(name = "comfort_level")
    private String comfortLevel;

    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;

    @Column(columnDefinition = "LONGTEXT")
    private String image1;

    @Column(columnDefinition = "LONGTEXT")
    private String image2;

    @Column(columnDefinition = "LONGTEXT")
    private String image3;

    @Column(columnDefinition = "LONGTEXT")
    private String image4;

    @Column(columnDefinition = "LONGTEXT")
    private String image5;
}
