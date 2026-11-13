import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import CitaForm from "./CitaForm.jsx";

const citaDeLas10 = {
  id: 7,
  fechaHoraInicio: "2030-01-16T10:00:00",
  cliente: { id: 1, nombre: "Laura", apellidos: "Pérez" },
  servicio: { id: 2, nombre: "Semipermanente" },
  trabajador: { id: 3, nombre: "Sindi", apellidos: "García" },
};

let citaEnviada;

const server = setupServer(
  http.get("*/api/admin/clientes", () => HttpResponse.json([citaDeLas10.cliente])),
  http.get("*/api/servicios", () => HttpResponse.json([citaDeLas10.servicio])),
  http.get("*/api/admin/trabajadores", () => HttpResponse.json([citaDeLas10.trabajador])),
  http.get("*/api/citas/disponibilidad", () => HttpResponse.json(["10:00", "10:15"])),
  http.put("*/api/admin/citas/:id", async ({ request }) => {
    citaEnviada = await request.json();
    return HttpResponse.json({});
  }),
);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => {
  server.resetHandlers();
  citaEnviada = undefined;
});
afterAll(() => server.close());

describe("CitaForm (panel de administración)", () => {
  it("guarda la cita con la hora local del salón, sin pasarla a UTC", async () => {
    const onSave = vi.fn();
    render(<CitaForm cita={citaDeLas10} onSave={onSave} onCancel={() => {}} />);

    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));

    await waitFor(() => expect(onSave).toHaveBeenCalled());
    expect(citaEnviada.fechaHoraInicio).toBe("2030-01-16T10:00:00");
  });
});
