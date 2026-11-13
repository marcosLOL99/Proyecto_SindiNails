import { render, screen } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { describe, expect, it } from "vitest";

import { AuthProvider } from "../context/AuthContext.jsx";
import ProtectedRoute from "./ProtectedRoute.jsx";

// Caracterización: documenta el comportamiento actual de las rutas de cliente.
const renderEnRuta = (rutaInicial) => {
  const router = createMemoryRouter(
    [
      {
        element: <ProtectedRoute />,
        children: [{ path: "/dashboard", element: <h1>Mi panel</h1> }],
      },
      { path: "/login", element: <h1>Iniciar sesión</h1> },
    ],
    { initialEntries: [rutaInicial] },
  );

  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  );
};

describe("ProtectedRoute", () => {
  it("sin sesión iniciada, redirige al login en vez de mostrar la página privada", async () => {
    renderEnRuta("/dashboard");

    expect(
      await screen.findByRole("heading", { name: "Iniciar sesión" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Mi panel" }),
    ).not.toBeInTheDocument();
  });
});
