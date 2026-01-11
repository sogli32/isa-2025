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
}

export interface CreateVideoRequest {
  title: string;
  description: string;
  tags: string;
  location?: string;
}
