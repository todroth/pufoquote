package net.droth.pufoquote.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the static Impressum page. */
@Controller
public class ImpressumController {

  @GetMapping("/impressum")
  public String impressum() {
    return "impressum";
  }
}
