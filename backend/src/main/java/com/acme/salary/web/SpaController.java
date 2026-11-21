package com.acme.salary.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Single-page-app fallback: the Angular router owns every path that is not
 * an API call and not a real static file (a file has an extension, e.g.
 * {@code main.js}). A direct load or refresh of {@code /dashboard},
 * {@code /employees/EMP-00001}, {@code /import}, … must serve
 * {@code index.html} so the Angular router can take over — otherwise
 * Spring answers 404 for a route that only exists client-side.
 *
 * <p>One method per path depth (1–3 segments — the deepest Angular route
 * is {@code /employees/:id/edit}). A {@code /**} catch-all is not usable
 * here: Spring's path parser forbids anything after {@code **}, and a
 * bare {@code /**} mapping would also swallow static resources.
 *
 * <p>Explicit mappings always win over these catch-alls: {@code /api/**}
 * is handled by the {@code @RestController}s, and real static files (they
 * contain a dot, which {@code [^\\.]+} excludes) are served by the
 * resource handler.
 */
@Controller
public class SpaController {

    @RequestMapping(value = "/{path:[^\\.]*}", method = RequestMethod.GET)
    public String forwardOneSegment() {
        return "forward:/index.html";
    }

    @RequestMapping(value = "/{a:[^\\.]*}/{b:[^\\.]*}", method = RequestMethod.GET)
    public String forwardTwoSegments() {
        return "forward:/index.html";
    }

    @RequestMapping(value = "/{a:[^\\.]*}/{b:[^\\.]*}/{c:[^\\.]*}", method = RequestMethod.GET)
    public String forwardThreeSegments() {
        return "forward:/index.html";
    }
}
