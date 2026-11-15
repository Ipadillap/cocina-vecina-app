package cl.udec.cocinavecina.dto;

import java.util.List;

public record ApiResponse (
    String title,
    int status,
    List<String> detail
){}
