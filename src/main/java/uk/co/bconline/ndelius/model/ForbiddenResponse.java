package uk.co.bconline.ndelius.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ForbiddenResponse
{
	private String user;
	private String[] requiredRoles;
}