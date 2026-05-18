package com.erp.epmapaApi.repositories;

import com.erp.epmapaApi.models.Tmpinteresxfac;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TminteresxfacR {
    Optional<Tmpinteresxfac> findByIdfactura(Long idfactura);

    List<Tmpinteresxfac> findAllByIdfacturaIn(Collection<Long> ids);
}
