package com.domain.literalura.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthorData(
        @JsonAlias("nome") String name,
        @JsonAlias("nascimento") String birth_year,
        @JsonAlias("falecimento") String death_year
) { }
