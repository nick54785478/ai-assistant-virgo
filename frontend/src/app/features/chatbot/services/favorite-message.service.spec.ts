import { TestBed } from '@angular/core/testing';

import { FavoriteMessageService } from './favorite-message.service';

describe('FavoriteMessageService', () => {
  let service: FavoriteMessageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FavoriteMessageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
