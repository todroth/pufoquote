package net.droth.pufoquote.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ImpressumController {

  @GetMapping("/impressum")
  public String impressum() {
    return "impressum";
  }
}
