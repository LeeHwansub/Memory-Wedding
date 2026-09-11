import { ProjectShell } from "@/components/shell/ProjectShell";

export default function ProjectLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <ProjectShell>{children}</ProjectShell>;
}
