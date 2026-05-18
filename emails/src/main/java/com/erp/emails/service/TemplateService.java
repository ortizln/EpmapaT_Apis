package com.erp.emails.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class TemplateService {

    private final TemplateEngine templateEngine;

    public TemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(String templateKey, Map<String, Object> variables) {
        Context ctx = new Context();
        if (variables != null) ctx.setVariables(variables);
        return templateEngine.process(templateKey, ctx);
    }
}