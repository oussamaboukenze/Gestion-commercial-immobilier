import { Box, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';

export default function PageHeader({ titre, sousTitre, actions }: { titre: string; sousTitre?: string; actions?: ReactNode }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', mb: 2, gap: 2, flexWrap: 'wrap' }}>
      <Box>
        <Typography variant="h5" fontWeight={600}>{titre}</Typography>
        {sousTitre && <Typography variant="body2" color="text.secondary">{sousTitre}</Typography>}
      </Box>
      {actions && <Stack direction="row" spacing={1}>{actions}</Stack>}
    </Box>
  );
}
