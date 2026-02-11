export interface Video {
  id: number;
  title: string;
  description: string;
  tags: string;
  location?: string;
  createdAt: string;
  viewCount: number;
  username: string;
  userId: number;

    likeCount?: number;
   likedByUser?: boolean;

  // Zakazano prikazivanje
  scheduledAt?: string;
  available?: boolean;
  streamOffsetSeconds?: number;
}

export interface CreateVideoRequest {
  title: string;
  description: string;
  tags: string;
  location?: string;
  scheduledAt?: string;
}

export interface StreamInfo {
  videoId: number;
  scheduled: boolean;
  available: boolean;
  scheduledAt?: string;
  streamOffsetSeconds?: number;
  secondsUntilAvailable?: number;
}
